# app.py
import hashlib
import os
import uuid
from datetime import datetime, timedelta, timezone
from functools import wraps
import jwt # PyJWT
import click # For CLI commands
import html # For escaping HTML
import json
import random # For 2FA code generation
import string # For 2FA code generation

from flask import (
    Flask, request, jsonify, redirect, url_for,
    flash, session, send_from_directory, abort, current_app, render_template
)
from flask_sqlalchemy import SQLAlchemy
from flask_migrate import Migrate # Flask-Migrate importu
from flask_mail import Mail, Message
from werkzeug.security import generate_password_hash, check_password_hash
from werkzeug.utils import secure_filename
from werkzeug.middleware.proxy_fix import ProxyFix
from threading import Thread
from flask_cors import CORS

# --- 0. Configuration ---
class Config:
    SECRET_KEY = os.environ.get('SECRET_KEY') or 'your-secret-key-no-iyzico-premium-questions'
    BASE_DIR = os.path.abspath(os.path.dirname(__file__))
    INSTANCE_FOLDER_PATH = os.path.join(BASE_DIR, 'instance')
    TEMPLATES_FOLDER = os.path.join(BASE_DIR, 'templates')
    STATIC_FOLDER = os.path.join(BASE_DIR, 'static')

    SQLALCHEMY_DATABASE_URI = os.environ.get('DATABASE_URL') or \
        'sqlite:///' + os.path.join(INSTANCE_FOLDER_PATH, 'sorugram_no_iyzico_premium_q.db')
    SQLALCHEMY_TRACK_MODIFICATIONS = False
    UPLOAD_FOLDER = os.path.join(STATIC_FOLDER, 'uploads', 'question_images')
    ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}
    API_BASE_URL = os.environ.get('API_BASE_URL') or "http://127.0.0.1:5000"
    APP_NAME = "Sorugram"

    MAIL_SERVER = os.environ.get('MAIL_SERVER') or 'smtp.gmail.com'
    MAIL_PORT = int(os.environ.get('MAIL_PORT') or 587)
    MAIL_USE_TLS = os.environ.get('MAIL_USE_TLS', 'true').lower() in ('true', '1', 't')
    MAIL_USERNAME = os.environ.get('MAIL_USERNAME') or 'mehmetalperkocaoglu@gmail.com'
    MAIL_PASSWORD = os.environ.get('MAIL_PASSWORD') or 'mabj hwdk zdbl ylgd' # GERÇEK ŞİFRENİZİ VEYA UYGULAMA ŞİFRESİNİ KULLANIN
    MAIL_DEFAULT_SENDER = (APP_NAME + ' Destek', MAIL_USERNAME)

    POINTS_CORRECT_ANSWER = 1000; POINTS_WRONG_ANSWER = -250; POINTS_BLANK_ANSWER = 0
    SHOW_AD_AFTER_N_ANSWERS = 1
    # CORS_ALLOWED_ORIGINS, Config dosyanızdan alınır.
    CORS_ALLOWED_ORIGINS = os.environ.get('CORS_ALLOWED_ORIGINS', 'http://localhost:8081,http://127.0.0.1:5000,null,http://192.168.31.6:5000,http://192.168.31.238:8081').split(',')


    PREMIUM_PRICE_MONTHLY_TRY = 30.00
    PREMIUM_DURATION_MONTHLY_DAYS = 30
    PREMIUM_PRICE_YEARLY_TRY = 300.00
    PREMIUM_DURATION_YEARLY_DAYS = 365

    ADMIN_IPS_FILE = os.path.join(INSTANCE_FOLDER_PATH, "admin_ips.txt") #
    USE_PROXY_FIX = False
    ADMIN_2FA_CODE_EXPIRATION_MINUTES = 10 #


    @staticmethod
    def create_folders():
        folders_to_create = [
            Config.INSTANCE_FOLDER_PATH, Config.UPLOAD_FOLDER,
            Config.TEMPLATES_FOLDER, os.path.join(Config.TEMPLATES_FOLDER, 'admin'),
            os.path.join(Config.TEMPLATES_FOLDER, 'includes'),
            Config.STATIC_FOLDER, os.path.join(Config.STATIC_FOLDER, 'css')
        ]
        for folder in folders_to_create:
            if not os.path.exists(folder):
                os.makedirs(folder); print(f"Created directory: {folder}")

Config.create_folders()

app = Flask(__name__, instance_path=Config.INSTANCE_FOLDER_PATH,
            template_folder=Config.TEMPLATES_FOLDER, static_folder=Config.STATIC_FOLDER)
app.config.from_object(Config)

if app.config.get('USE_PROXY_FIX', False):
    app.wsgi_app = ProxyFix(app.wsgi_app, x_for=1, x_proto=1, x_host=1, x_port=1, x_prefix=1)

CORS(app, resources={r"/api/*": {"origins": app.config.get("CORS_ALLOWED_ORIGINS")}}) #

db = SQLAlchemy(app)
migrate = Migrate(app, db) # Migrate nesnesi burada oluşturulmalı
mail = Mail(app)

# --- IP Kısıtlama Fonksiyonu (Adminler için, dosya tabanlı hash kontrolü) ---
@app.before_request
def limit_admin_access_by_ip(): #
    if request.path.startswith('/admin'):
        client_ip = request.remote_addr
        try:
            hashed_client_ip = hashlib.sha256(client_ip.encode('utf-8')).hexdigest()
        except Exception as e:
            current_app.logger.error(f"İstemci IP'si hashlenirken hata: {client_ip}, Hata: {e}")
            abort(403)
            return

        allowed_ip_hashes = set()
        admin_ips_file_path = current_app.config['ADMIN_IPS_FILE']

        if os.path.exists(admin_ips_file_path):
            try:
                with open(admin_ips_file_path, 'r', encoding='utf-8') as f:
                    for line in f:
                        line = line.strip()
                        if line and ':' in line:
                            try:
                                _name, hashed_ip_val = line.split(':', 1)
                                allowed_ip_hashes.add(hashed_ip_val.strip())
                            except ValueError:
                                current_app.logger.warning(f"Admin IP dosyasında hatalı formatlı satır: '{line}', atlanıyor.")
            except Exception as e:
                current_app.logger.error(f"Admin IP dosyası ({admin_ips_file_path}) okunurken hata: {e}")
                if current_app.debug and client_ip == '127.0.0.1':
                    current_app.logger.warning(f"Admin IP dosyası okunamadı, ancak yerel geliştirme için {client_ip} IP'sine {request.path} yoluna izin verildi.")
                    return
                abort(403)
        
        if not allowed_ip_hashes:
            if current_app.debug and client_ip == '127.0.0.1':
                current_app.logger.info(f"Admin IP listesi boş (dosya eksik/boş/okunaksız), ancak yerel geliştirme için {client_ip} IP'sine {request.path} yoluna izin verildi.")
                return
            current_app.logger.warning(f"Admin IP listesi boş. IP {client_ip} (Hash: {hashed_client_ip}) için {request.path} yoluna admin erişimi engellendi.")
            abort(403)

        if hashed_client_ip not in allowed_ip_hashes:
            current_app.logger.warning(f"Admin paneline yetkisiz IP'den ({client_ip}, Hash: {hashed_client_ip}) {request.path} yoluna erişim denemesi engellendi. İzin verilen hash listesinde değil.")
            abort(403)

# --- 1. Database Models ---
class User(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(80), unique=True, nullable=False)
    email = db.Column(db.String(120), unique=True, nullable=False)
    password_hash = db.Column(db.String(256), nullable=False)
    email_verified = db.Column(db.Boolean, default=False, nullable=False)
    is_admin = db.Column(db.Boolean, default=False, nullable=False)
    is_active = db.Column(db.Boolean, default=True, nullable=False)  # YENİ: Kullanıcı aktiflik durumu
    score = db.Column(db.Integer, default=0, nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    is_premium = db.Column(db.Boolean, default=False, nullable=False)
    premium_expiration_date = db.Column(db.DateTime, nullable=True)
    submitted_questions = db.relationship('Question', foreign_keys='Question.submitted_by_user_id', backref='submitter', lazy=True)
    answers = db.relationship('UserAnswer', backref='user', lazy=True)
    reports = db.relationship('QuestionReport', backref='reporter', lazy=True)

    def set_password(self, password): self.password_hash = generate_password_hash(password)
    def check_password(self, password): return check_password_hash(self.password_hash, password)
    def get_email_verification_token(self, expires_in=3600):
        return jwt.encode({"verify_email": self.id, "exp": datetime.now(timezone.utc) + timedelta(seconds=expires_in)}, current_app.config['SECRET_KEY'], algorithm="HS256")
    @staticmethod
    def verify_email_verification_token(token):
        try:
            payload = jwt.decode(token, current_app.config['SECRET_KEY'], algorithms=["HS256"])
            user_id = payload.get('verify_email'); return User.query.get(user_id) if user_id else None
        except jwt.ExpiredSignatureError: return 'expired'
        except jwt.InvalidTokenError: return 'invalid'
        except Exception: return None
    @property
    def has_active_premium(self):
        if self.is_premium and self.premium_expiration_date:
            exp_date_aware = self.premium_expiration_date
            if exp_date_aware.tzinfo is None or exp_date_aware.tzinfo.utcoffset(exp_date_aware) is None:
                exp_date_aware = exp_date_aware.replace(tzinfo=timezone.utc)
            return datetime.now(timezone.utc) < exp_date_aware
        return False
    def __repr__(self): return f'<User {self.username}>'

# ... (Course, Question, UserAnswer, QuestionReport, Payment modelleri aynı kalacak) ...
class Course(db.Model):
    id = db.Column(db.Integer, primary_key=True); name = db.Column(db.String(100), unique=True, nullable=False)
    questions = db.relationship('Question', backref='course', lazy='dynamic')
    def __repr__(self): return f'<Course {self.name}>'

class Question(db.Model):
    id = db.Column(db.Integer, primary_key=True); course_id = db.Column(db.Integer, db.ForeignKey('course.id'), nullable=False)
    text = db.Column(db.Text, nullable=False); option_a = db.Column(db.String(255), nullable=False)
    option_b = db.Column(db.String(255), nullable=False); option_c = db.Column(db.String(255), nullable=False)
    option_d = db.Column(db.String(255), nullable=False); option_e = db.Column(db.String(255), nullable=False)
    correct_option = db.Column(db.String(1), nullable=False); image_filename = db.Column(db.String(255), nullable=True)
    link_id = db.Column(db.String(36), unique=True, nullable=False, default=lambda: str(uuid.uuid4()))
    status = db.Column(db.String(20), default='pending', nullable=False)
    submitted_by_user_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=True)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    is_premium_only = db.Column(db.Boolean, default=False, nullable=False) 
    answers = db.relationship('UserAnswer', backref='question', lazy=True)
    reports = db.relationship('QuestionReport', backref='question', lazy=True)
    def get_options_dict(self): return {"A": self.option_a, "B": self.option_b, "C": self.option_c, "D": self.option_d, "E": self.option_e}
    def get_image_url(self): return url_for('uploaded_file', filename=self.image_filename, _external=True) if self.image_filename else None
    def __repr__(self): return f'<Question {self.id}: {self.text[:30]}...>'

class UserAnswer(db.Model):
    id = db.Column(db.Integer, primary_key=True); user_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    question_id = db.Column(db.Integer, db.ForeignKey('question.id'), nullable=False)
    selected_option = db.Column(db.String(1), nullable=False); is_correct = db.Column(db.Boolean, nullable=True)
    timestamp = db.Column(db.DateTime, default=datetime.utcnow)
    def __repr__(self): return f'<UserAnswer by {self.user_id} for Q{self.question_id}>'

class QuestionReport(db.Model):
    id = db.Column(db.Integer, primary_key=True); question_id = db.Column(db.Integer, db.ForeignKey('question.id'), nullable=False)
    reported_by_user_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    reason = db.Column(db.Text, nullable=True); status = db.Column(db.String(20), default='new', nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    def __repr__(self): return f'<QuestionReport {self.id} for Q{self.question_id}>'

class Payment(db.Model): 
    id = db.Column(db.Integer, primary_key=True); user_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    payment_gateway_transaction_id = db.Column(db.String(120), unique=True, nullable=False) 
    conversation_id = db.Column(db.String(120), nullable=True) 
    plan_type = db.Column(db.String(20), nullable=False); amount_paid = db.Column(db.Numeric(10, 2), nullable=False)
    currency = db.Column(db.String(3), nullable=False, default='TRY'); status = db.Column(db.String(30), nullable=False, default='succeeded')
    created_at = db.Column(db.DateTime, default=lambda: datetime.now(timezone.utc)) 
    updated_at = db.Column(db.DateTime, default=lambda: datetime.now(timezone.utc), onupdate=lambda: datetime.now(timezone.utc))
    user = db.relationship('User', backref=db.backref('payments', lazy='dynamic'))
    def __repr__(self): return f'<Payment {self.id} - PGTID: {self.payment_gateway_transaction_id}>'


# --- 2. Utility Functions ---
# ... (send_async_email, send_verification_email, send_payment_receipt_email, allowed_file, generate_verification_code, send_admin_login_code_email aynı kalacak) ...
def send_async_email(flask_app, msg_params):
    with flask_app.app_context():
        try: msg = Message(**msg_params); mail.send(msg); flask_app.logger.info(f"Async email sent to {msg_params['recipients']}")
        except Exception as e: flask_app.logger.error(f"Failed to send async email: {e}")

def send_verification_email(to_email, user_username, verify_url):
    flask_app = current_app._get_current_object()
    subject = f"{flask_app.config['APP_NAME']} E-posta Doğrulama"
    html_body = render_template('email_verification.html', user_username=user_username, 
                                verify_url=verify_url, current_year=datetime.utcnow().year)
    msg_params = { 'subject': subject, 'recipients': [to_email], 'html': html_body, 'sender': flask_app.config['MAIL_DEFAULT_SENDER'] }
    Thread(target=send_async_email, args=[flask_app, msg_params]).start()

def send_payment_receipt_email(user_obj, payment_obj):
    flask_app = current_app._get_current_object()
    subject = f"{flask_app.config['APP_NAME']} - Premium Üyelik Makbuzu ({payment_obj.payment_gateway_transaction_id})"
    html_body = render_template('email_payment_receipt.html', user=user_obj, payment=payment_obj)
    msg_params = { 'subject': subject, 'recipients': [user_obj.email], 'html': html_body, 'sender': flask_app.config['MAIL_DEFAULT_SENDER'] }
    Thread(target=send_async_email, args=[flask_app, msg_params]).start()
    flask_app.logger.info(f"Queued payment receipt email for {user_obj.email} for payment {payment_obj.id}.")

def allowed_file(filename): return '.' in filename and filename.rsplit('.', 1)[1].lower() in current_app.config['ALLOWED_EXTENSIONS']

def generate_verification_code(length=6):
    return "".join(random.choices(string.digits, k=length))

def send_admin_login_code_email(to_email, username, code):
    flask_app = current_app._get_current_object()
    subject = f"{flask_app.config['APP_NAME']} Admin Giriş Kodu"
    html_body = render_template('email_admin_login_code.html',
                                user_username=username,
                                login_code=code,
                                app_name=flask_app.config['APP_NAME'],
                                expiration_minutes=flask_app.config['ADMIN_2FA_CODE_EXPIRATION_MINUTES'])
    msg_params = {
        'subject': subject,
        'recipients': [to_email],
        'html': html_body,
        'sender': flask_app.config['MAIL_DEFAULT_SENDER']
    }
    try:
        Thread(target=send_async_email, args=[flask_app, msg_params]).start()
        flask_app.logger.info(f"Admin login 2FA code sent to {to_email}")
    except Exception as e:
        flask_app.logger.error(f"Failed to send admin login 2FA code email to {to_email}: {e}")
        raise

# --- 3. Helper Decorators & Route Logic ---
def token_required(f): # Kullanıcı API'leri için
    @wraps(f)
    def decorated(*args, **kwargs):
        token = None
        auth_header = request.headers.get('Authorization')
        if auth_header:
            try:
                token = auth_header.split(" ")[1]
            except IndexError:
                return jsonify({'message': 'Bearer token hatalı biçimlendirilmiş.'}), 401
        
        if not token:
            return jsonify({'message': 'Token eksik!'}), 401
        
        try:
            data = jwt.decode(token, current_app.config['SECRET_KEY'], algorithms=["HS256"])
            # `current_user` yerine `current_user_obj` kullanalım ki admin rotalarındaki `current_admin_user` ile karışmasın
            current_user_obj = User.query.get(data['user_id'])
            if not current_user_obj:
                raise Exception("Kullanıcı bulunamadı")
            if not current_user_obj.is_active: # Token geçerli olsa bile kullanıcı aktif değilse
                 return jsonify({'message': 'Hesabınız aktif değil.'}), 403

        except jwt.ExpiredSignatureError:
            return jsonify({'message': 'Token süresi dolmuş!'}), 401
        except Exception as e:
            current_app.logger.error(f"Token doğrulama hatası: {e}")
            return jsonify({'message': 'Token geçersiz!'}), 401
        
        return f(current_user_obj, *args, **kwargs) # Değiştirilmiş kullanıcı objesini pass et
    return decorated

def admin_required(f): # Admin paneli rotaları için
    @wraps(f)
    def decorated(*args, **kwargs):
        if 'admin_id' not in session:
            flash('Bu sayfaya erişmek için admin girişi yapmanız gerekmektedir.', 'warning')
            return redirect(url_for('admin_login'))
        
        admin_user_from_session = User.query.get(session['admin_id'])
        
        if not admin_user_from_session or not admin_user_from_session.is_admin:
            flash('Bu sayfaya erişim yetkiniz yok.', 'danger')
            session.pop('admin_id', None)
            return redirect(url_for('admin_login'))
        
        # YENİ KONTROL: Admin kullanıcısı aktif mi?
        if not admin_user_from_session.is_active:
            flash('Hesabınız aktif değil. Bu sayfaya erişim yetkiniz yok.', 'danger')
            session.pop('admin_id', None) # Aktif olmayan kullanıcı için session'ı temizle
            current_app.logger.warning(f"Devre dışı bırakılmış admin ({admin_user_from_session.username}) yetkili bir sayfaya erişmeye çalıştı.")
            return redirect(url_for('admin_login'))
            
        return f(admin_user_from_session, *args, **kwargs)
    return decorated

@app.route('/static/uploads/question_images/<filename>')
def uploaded_file(filename): return send_from_directory(current_app.config['UPLOAD_FOLDER'], filename)

# --- Public Web Page Routes ---
# ... (/, /login, /register, /premium-info rotaları aynı kalacak) ...
@app.route('/')
def index_page(): return render_template('index.html')
@app.route('/login')
def login_page(): return render_template('login.html')
@app.route('/register')
def register_page(): return render_template('register.html')
@app.route('/premium-info')
def premium_info_page(): return render_template('premium_info.html', title="Premium Üyelik")

# --- API Endpoints ---
# current_user parametreleri current_user_obj olarak güncellendi
@app.route('/api/register', methods=['POST'])
def register_user_api():
    data = request.get_json()
    if not data or not data.get('username') or not data.get('email') or not data.get('password'):
        return jsonify({'message': 'Kullanıcı adı, e-posta ve şifre gereklidir.'}), 400
    if User.query.filter_by(username=data['username']).first(): return jsonify({'message': 'Bu kullanıcı adı zaten alınmış.'}), 409
    if User.query.filter_by(email=data['email']).first(): return jsonify({'message': 'Bu e-posta adresi zaten kayıtlı.'}), 409
    if len(data['password']) < 6: return jsonify({'message': 'Şifre en az 6 karakter olmalıdır.'}), 400
    # Yeni kullanıcılar varsayılan olarak is_active=True olur (modelde default=True)
    user = User(username=data['username'], email=data['email']); user.set_password(data['password'])
    db.session.add(user); db.session.commit()
    try:
        token = user.get_email_verification_token()
        verify_url = url_for('verify_email_route_html', token=token, _external=True)
        send_verification_email(user.email, user.username, verify_url)
    except Exception as e: current_app.logger.error(f"Doğrulama e-postası gönderilemedi {user.email}: {e}")
    return jsonify({'message': 'Kullanıcı başarıyla kaydedildi. Lütfen e-postanızı doğrulayın.'}), 201

@app.route('/api/login', methods=['POST'])
def login_user_api():
    data = request.get_json(); identifier = data.get('identifier') or data.get('email')
    if not data or not identifier or not data.get('password'): return jsonify({'message': 'E-posta/Kullanıcı adı ve şifre gereklidir.'}), 400
    user = User.query.filter((User.email == identifier) | (User.username == identifier)).first()
    if not user or not user.check_password(data['password']): return jsonify({'message': 'Geçersiz e-posta/kullanıcı adı veya şifre.'}), 401
    
    # Aktiflik kontrolü (admin olmayan kullanıcılar için)
    if not user.is_active:
        return jsonify({'message': 'Hesabınız devre dışı bırakılmış.'}), 403

    if not user.email_verified and not user.is_admin: # Admin olmayanlar için e-posta doğrulama
        return jsonify({'message': 'Hesap doğrulanmamış. Lütfen e-postanızı kontrol edin.','email_not_verified': True, 'email': user.email }), 403
    
    if user.is_premium and user.premium_expiration_date:
        exp_date_aware = user.premium_expiration_date
        if exp_date_aware.tzinfo is None or exp_date_aware.tzinfo.utcoffset(exp_date_aware) is None:
            exp_date_aware = exp_date_aware.replace(tzinfo=timezone.utc)
        if datetime.now(timezone.utc) > exp_date_aware: user.is_premium = False
    
    token = jwt.encode({ 'user_id': user.id, 'is_admin': user.is_admin, 'exp': datetime.now(timezone.utc) + timedelta(hours=24) }, current_app.config['SECRET_KEY'], algorithm="HS256")
    db.session.commit()
    return jsonify({ 'message': 'Giriş başarılı.', 'token': token, 'user': {
        'id': user.id, 'username': user.username, 'email': user.email, 'is_admin': user.is_admin,
        'email_verified': user.email_verified, 'score': user.score, 'is_premium': user.is_premium,
        'has_active_premium': user.has_active_premium, 'is_active': user.is_active, # is_active eklendi
        'premium_expiration_date': user.premium_expiration_date.isoformat() if user.premium_expiration_date else None }}), 200

@app.route('/verify-email/<token>')
def verify_email_route_html(token): 
    user_or_status = User.verify_email_verification_token(token)
    status_message = ""; status_icon = ""; status_title = ""; is_success = False
    if user_or_status == 'expired': status_title = "Bağlantı Süresi Dolmuş!"; status_message = "Doğrulama bağlantısının süresi dolmuş."; status_icon = "&#9200;"
    elif user_or_status == 'invalid' or user_or_status is None: status_title = "Doğrulama Başarısız!"; status_message = "Geçersiz doğrulama bağlantısı."; status_icon = "&#10060;"
    else: 
        user = user_or_status; is_success = True; status_icon = "&#10004;"
        if user.email_verified: status_title = "E-posta Zaten Doğrulanmış!"; status_message = f"Merhaba {html.escape(user.username)}, e-posta adresiniz zaten doğrulanmış."
        else: 
            user.email_verified = True
            user.is_active = True # E-posta doğrulandığında kullanıcıyı aktif et
            db.session.commit()
            status_title = "E-posta Başarıyla Doğrulandı!"; status_message = f"Merhaba {html.escape(user.username)}, e-posta adresiniz başarıyla doğrulandı ve hesabınız aktifleştirildi!"
    return render_template('email_verified_page.html', status_title=status_title, status_message=status_message, status_icon=status_icon, is_success=is_success, app_deep_link=f"sorugramapp://login")

# ... (diğer API endpointleri aynı, current_user -> current_user_obj değişikliği uygulandı) ...
@app.route('/api/questions/share/<link_id>', methods=['GET']) 
def get_shared_question_api_or_html(link_id):
    question = Question.query.filter_by(link_id=link_id, status='approved').first()
    if not question:
        if 'text/html' in request.accept_mimetypes: return render_template('shared_question_page.html', question=None, error_message="Soru bulunamadı veya onaylanmamış."), 404
        return jsonify({'message': 'Soru bulunamadı veya onaylanmamış.'}), 404
    course = Course.query.get(question.course_id)
    question_data = { 'id': question.id, 'text': question.text, 'options': question.get_options_dict(), 'image_url': question.get_image_url(), 'link_id': question.link_id, 'course_name': course.name if course else "Bilinmeyen Ders", 'course_id': question.course_id }
    if 'text/html' in request.accept_mimetypes: return render_template('shared_question_page.html', question=question_data, app_deep_link=f"sorugramapp://question/{question.link_id}")
    return jsonify(question_data), 200

@app.route('/api/courses', methods=['GET']) 
@token_required
def get_courses_api(current_user_obj): # Değiştirildi
    if not current_user_obj.email_verified: return jsonify({'message': 'Lütfen e-postanızı doğrulayın.'}), 403
    courses = Course.query.order_by(Course.name).all()
    return jsonify([{'id': course.id, 'name': course.name} for course in courses]), 200

@app.route('/api/questions/<int:course_id>', methods=['GET'])
@token_required
def get_questions_for_course_api(current_user_obj, course_id): # Değiştirildi
    if not current_user_obj.email_verified:
         return jsonify({'message': 'Soruları görmek için lütfen e-postanızı doğrulayın.'}), 403
    course = Course.query.get_or_404(course_id)
    answered_correctly_subquery = db.session.query(UserAnswer.question_id).filter(UserAnswer.user_id == current_user_obj.id, UserAnswer.is_correct == True, Question.course_id == course_id).subquery()
    query_filter = [Question.course_id == course_id, Question.status == 'approved', ~Question.id.in_(answered_correctly_subquery)]
    if not current_user_obj.has_active_premium:
        query_filter.append(Question.is_premium_only == False)
    question = Question.query.filter(*query_filter).order_by(db.func.random()).first()
    if not question:
        base_query_all_relevant = Question.query.filter(Question.course_id == course_id, Question.status == 'approved')
        if not current_user_obj.has_active_premium: base_query_all_relevant = base_query_all_relevant.filter(Question.is_premium_only == False)
        all_relevant_q_count = base_query_all_relevant.count()
        correct_ans_query_relevant = UserAnswer.query.join(Question).filter(UserAnswer.user_id == current_user_obj.id, UserAnswer.is_correct == True, Question.course_id == course_id)
        if not current_user_obj.has_active_premium: correct_ans_query_relevant = correct_ans_query_relevant.filter(Question.is_premium_only == False)
        correct_ans_relevant_count = correct_ans_query_relevant.count()
        if all_relevant_q_count > 0 and all_relevant_q_count == correct_ans_relevant_count:
             return jsonify({'message': f'{course.name} dersindeki size uygun tüm soruları doğru yanıtladınız! Tebrikler!'}), 404
        if not current_user_obj.has_active_premium and Question.query.filter(Question.course_id == course_id, Question.status == 'approved', Question.is_premium_only == True).first():
            return jsonify({'message': f'{course.name} dersinde size uygun soru bulunamadı. Daha fazla soru için premium üyeliği düşünebilirsiniz.'}), 404
        return jsonify({'message': f'{course.name} dersinde uygun soru bulunamadı veya tüm soruları yanıtladınız.'}), 404
    return jsonify({ 'id': question.id, 'text': question.text, 'options': question.get_options_dict(), 'image_url': question.get_image_url(), 'link_id': question.link_id, 'course': course.name, 'course_id': course.id, 'is_premium_only': question.is_premium_only }), 200

@app.route('/api/submit-answer', methods=['POST'])
@token_required
def submit_answer_api(current_user_obj): # Değiştirildi
    if not current_user_obj.email_verified: return jsonify({'message': 'Lütfen e-postanızı doğrulayın.'}), 403
    data = request.get_json(); question_id = data.get('question_id'); selected_option = data.get('selected_option', '').upper()
    if not question_id or not selected_option or selected_option not in ['A', 'B', 'C', 'D', 'E', 'F']:
        return jsonify({'message': 'Soru ID ve geçerli seçenek (A-F) gereklidir.'}), 400
    question = Question.query.get(question_id)
    if not question or question.status != 'approved': return jsonify({'message': 'Soru bulunamadı.'}), 404
    if UserAnswer.query.filter_by(user_id=current_user_obj.id, question_id=question_id, is_correct=True).first():
        return jsonify({ 'message': 'Bu soruyu zaten doğru cevapladınız.', 'is_correct': True, 'correct_option': question.correct_option.upper(), 'new_score': current_user_obj.score, 'show_ad_trigger': False }), 200
    is_correct, points_gained, feedback_message = None, 0, ""
    if selected_option == 'F': is_correct = None; points_gained = current_app.config['POINTS_BLANK_ANSWER']; feedback_message = "Soru boş bırakıldı."
    else:
        is_correct = (selected_option == question.correct_option.upper())
        points_gained = current_app.config['POINTS_CORRECT_ANSWER'] if is_correct else current_app.config['POINTS_WRONG_ANSWER']
        feedback_message = "Doğru cevap!" if is_correct else f"Yanlış cevap. Doğru: {question.correct_option.upper()}"
    user_answer = UserAnswer(user_id=current_user_obj.id, question_id=question.id, selected_option=selected_option, is_correct=is_correct)
    db.session.add(user_answer)
    if selected_option != 'F': current_user_obj.score = max(0, (current_user_obj.score or 0) + points_gained)
    db.session.commit()
    total_answers_by_user = UserAnswer.query.filter_by(user_id=current_user_obj.id).count()
    show_ad = (not current_user_obj.has_active_premium) and (total_answers_by_user % current_app.config['SHOW_AD_AFTER_N_ANSWERS'] == 0) and selected_option != 'F'
    return jsonify({ 'message': feedback_message, 'is_correct': is_correct, 'correct_option': question.correct_option.upper(), 'new_score': current_user_obj.score, 'show_ad_trigger': show_ad }), 200

@app.route('/api/questions/report', methods=['POST'])
@token_required
def report_question_api(current_user_obj): # Değiştirildi
    data = request.get_json(); question_id = data.get('question_id'); reason = data.get('reason', '')
    if not question_id: return jsonify({'message': 'Soru ID gereklidir.'}), 400
    question = Question.query.get(question_id)
    if not question: return jsonify({'message': 'Bildirilecek soru bulunamadı.'}), 404
    if QuestionReport.query.filter_by(question_id=question_id, reported_by_user_id=current_user_obj.id).first():
        return jsonify({'message': 'Bu soruyu zaten bildirdiniz.'}), 409
    report = QuestionReport(question_id=question_id, reported_by_user_id=current_user_obj.id, reason=reason, status='new')
    if question.status == 'approved': question.status = 'reported'; question.updated_at = datetime.utcnow()
    db.session.add(report); db.session.commit()
    return jsonify({'message': 'Soru başarıyla bildirildi. İncelenecektir.'}), 200

@app.route('/api/user/stats', methods=['GET'])
@token_required
def get_user_stats_api(current_user_obj): # Değiştirildi
    if current_user_obj.is_premium and current_user_obj.premium_expiration_date:
        exp_date_aware = current_user_obj.premium_expiration_date 
        if exp_date_aware.tzinfo is None: exp_date_aware = exp_date_aware.replace(tzinfo=timezone.utc)
        if datetime.now(timezone.utc) > exp_date_aware: current_user_obj.is_premium = False; db.session.commit()
    total_answered = UserAnswer.query.filter_by(user_id=current_user_obj.id).count()
    correct_answers = UserAnswer.query.filter_by(user_id=current_user_obj.id, is_correct=True).count()
    incorrect_answers = UserAnswer.query.filter_by(user_id=current_user_obj.id, is_correct=False).count()
    blank_answers = UserAnswer.query.filter_by(user_id=current_user_obj.id, selected_option='F').count()
    answered_non_blank = total_answered - blank_answers
    accuracy_excluding_blanks = (correct_answers / answered_non_blank * 100) if answered_non_blank > 0 else 0
    rates = {}
    if total_answered > 0:
        rates['true'] = round((correct_answers / total_answered) * 100, 2); rates['false'] = round((incorrect_answers / total_answered) * 100, 2)
        rates['blank'] = round((blank_answers / total_answered) * 100, 2)
    return jsonify({ 'username': current_user_obj.username, 'score': current_user_obj.score,
                     'total_answered_questions': total_answered, 'correctly_answered_questions': correct_answers,
                     'incorrectly_answered_questions': incorrect_answers, 'blank_answered_questions': blank_answers,
                     'accuracy_rate_excluding_blanks': round(accuracy_excluding_blanks, 2), 'rates': rates,
                     'email_verified': current_user_obj.email_verified, 'is_premium': current_user_obj.is_premium,
                     'has_active_premium': current_user_obj.has_active_premium, 'is_active': current_user_obj.is_active, # is_active eklendi
                     'premium_expiration_date': current_user_obj.premium_expiration_date.isoformat() if current_user_obj.premium_expiration_date else None
                   }), 200

@app.route('/api/questions/submit', methods=['POST'])
@token_required
def submit_new_question_api(current_user_obj): # Değiştirildi
    if not current_user_obj.email_verified: return jsonify({'message': 'Lütfen e-postanızı doğrulayın.'}), 403
    required_fields = ['course_id', 'text', 'option_a', 'option_b', 'option_c', 'option_d', 'option_e', 'correct_option']
    if not all(field in request.form for field in required_fields):
        return jsonify({'message': f'Eksik form verisi: {", ".join(f for f in required_fields if f not in request.form)}'}), 400
    correct_option = request.form['correct_option'].upper()
    if correct_option not in ['A', 'B', 'C', 'D', 'E']: return jsonify({'message': 'Geçersiz doğru seçenek.'}), 400
    course = Course.query.get(request.form['course_id'])
    if not course: return jsonify({'message': 'Ders bulunamadı.'}), 404
    filename = None
    if 'photo' in request.files:
        file = request.files['photo']
        if file and file.filename != '' and allowed_file(file.filename):
            filename = secure_filename(f"{uuid.uuid4()}_{file.filename}")
            file.save(os.path.join(current_app.config['UPLOAD_FOLDER'], filename))
        elif file.filename != '': return jsonify({'message': 'İzin verilmeyen dosya türü.'}), 400
    
    is_premium_only_from_form = request.form.get('is_premium_only', 'false').lower() == 'true'

    new_question = Question( course_id=request.form['course_id'], text=request.form['text'],
                             option_a=request.form['option_a'], option_b=request.form['option_b'],
                             option_c=request.form['option_c'], option_d=request.form['option_d'],
                             option_e=request.form['option_e'], correct_option=correct_option,
                             image_filename=filename, status='pending', 
                             submitted_by_user_id=current_user_obj.id,
                             is_premium_only=is_premium_only_from_form )
    db.session.add(new_question); db.session.commit()
    return jsonify({'message': 'Sorunuz onay için başarıyla gönderildi!'}), 201

@app.route('/api/resend-verification-email', methods=['POST'])
def resend_verification_email_api():
    data = request.get_json(); email_to_verify = None; user_for_email = None; current_user_obj_from_token = None
    auth_header = request.headers.get('Authorization')
    if auth_header:
        try:
            token = auth_header.split(" ")[1]
            payload = jwt.decode(token, current_app.config['SECRET_KEY'], algorithms=["HS256"], options={"verify_exp": False})
            user_from_token = User.query.get(payload.get('user_id'))
            if user_from_token: current_user_obj_from_token = user_from_token
        except Exception: pass
    if current_user_obj_from_token:
        if current_user_obj_from_token.email_verified: return jsonify({'message': 'E-posta adresiniz zaten doğrulanmış.'}), 400
        user_for_email = current_user_obj_from_token
    elif data and data.get('email'):
        email_to_verify = data.get('email')
        user_for_email = User.query.filter_by(email=email_to_verify).first()
        if not user_for_email: return jsonify({'message': 'Bu e-posta adresi ile kayıtlı kullanıcı bulunamadı.'}), 404
        if user_for_email.email_verified: return jsonify({'message': 'Bu e-posta adresi zaten doğrulanmış.'}), 400
    else: return jsonify({'message': 'E-posta adresi veya geçerli token gereklidir.'}), 400
    try:
        verification_jwt_token = user_for_email.get_email_verification_token()
        verify_url = url_for('verify_email_route_html', token=verification_jwt_token, _external=True)
        send_verification_email(user_for_email.email, user_for_email.username, verify_url)
        return jsonify({'message': f'{user_for_email.email} adresine yeni doğrulama e-postası gönderildi.'}), 200
    except Exception as e:
        current_app.logger.error(f"Doğrulama e-postası tekrar gönderilemedi {user_for_email.email}: {e}")
        return jsonify({'message': 'Doğrulama e-postası gönderilirken bir hata oluştu.'}), 500

@app.route('/api/user/subscribe-premium', methods=['POST'])
@token_required
def subscribe_premium_api(current_user_obj): # Değiştirildi
    if not current_user_obj.email_verified:
        return jsonify({'message': 'Premium üyelik için önce e-postanızı doğrulamanız gerekmektedir.'}), 403
    if current_user_obj.has_active_premium:
        exp_date_str = current_user_obj.premium_expiration_date.strftime("%Y-%m-%d %H:%M UTC") if current_user_obj.premium_expiration_date else "N/A"
        return jsonify({'message': f'Zaten aktif bir premium üyeliğiniz var. Bitiş tarihi: {exp_date_str}.'}), 409
    data = request.get_json()
    if not data or 'plan_type' not in data:
        return jsonify({'message': 'Plan tipi (plan_type) gereklidir: "monthly" veya "yearly".'}), 400
    plan_type = data.get('plan_type').lower()
    duration_days = 0; amount_charged = 0.0
    if plan_type == 'monthly':
        duration_days = current_app.config['PREMIUM_DURATION_MONTHLY_DAYS']
        amount_charged = current_app.config['PREMIUM_PRICE_MONTHLY_TRY']
    elif plan_type == 'yearly':
        duration_days = current_app.config['PREMIUM_DURATION_YEARLY_DAYS']
        amount_charged = current_app.config['PREMIUM_PRICE_YEARLY_TRY']
    else: return jsonify({'message': 'Geçersiz plan tipi. "monthly" veya "yearly" olmalıdır.'}), 400
    current_user_obj.is_premium = True
    current_user_obj.premium_expiration_date = datetime.now(timezone.utc) + timedelta(days=duration_days)
    simulated_transaction_id = f"SIM_{plan_type.upper()}_{str(uuid.uuid4())[:12]}"
    new_payment = Payment(user_id=current_user_obj.id, payment_gateway_transaction_id=simulated_transaction_id,
                          plan_type=plan_type, amount_paid=amount_charged, currency='TRY', status='succeeded')
    db.session.add(new_payment); db.session.commit()
    current_app.logger.info(f"Kullanıcı {current_user_obj.id} için SİMÜLE EDİLMİŞ premium ({plan_type}) aktifleştirildi. İşlem ID: {simulated_transaction_id}")
    try: send_payment_receipt_email(current_user_obj, new_payment)
    except Exception as mail_e: current_app.logger.error(f"Makbuz e-postası gönderilemedi: {mail_e}")
    return jsonify({ 'message': f'{plan_type.capitalize()} premium üyeliğiniz başarıyla (simüle edilerek) aktifleştirildi!',
                     'is_premium': current_user_obj.is_premium, 'has_active_premium': current_user_obj.has_active_premium,
                     'premium_expiration_date': current_user_obj.premium_expiration_date.isoformat(),
                     'simulated_transaction_id': simulated_transaction_id }), 200

# --- CANLI İSTATİSTİK ROTASI ---
@app.route('/live')
def live_stats_page():
    total_users = User.query.count()
    now_utc = datetime.now(timezone.utc)
    active_premium_users = User.query.filter(
        User.is_premium == True,
        User.premium_expiration_date != None,
        User.premium_expiration_date > now_utc
    ).count()
    total_questions = Question.query.count()
    approved_questions_count = Question.query.filter_by(status='approved').count()
    total_premium_questions = Question.query.filter_by(is_premium_only=True, status='approved').count()
    top_10_users = User.query.filter(User.is_admin == False).order_by(User.score.desc()).limit(10).all()
    top_10_users_with_premium_status = []
    for user_stat in top_10_users:
        top_10_users_with_premium_status.append({
            'username': html.escape(user_stat.username),
            'score': user_stat.score,
            'is_premium': user_stat.has_active_premium
        })
    stats_data = {
        'total_users': total_users,
        'active_premium_users': active_premium_users,
        'total_questions': total_questions,
        'approved_questions_count': approved_questions_count,
        'total_premium_questions': total_premium_questions,
        'top_10_users': top_10_users_with_premium_status
    }
    admin_link_visible = False
    if 'admin_id' in session:
        admin_user_session = User.query.get(session['admin_id'])
        if admin_user_session and admin_user_session.is_admin and admin_user_session.is_active: # Aktiflik kontrolü eklendi
            admin_link_visible = True
    return render_template('live_stats.html', stats=stats_data, admin_user_link_visible=admin_link_visible)


# --- Admin Panel Routes (IP Kısıtlaması + 2FA ile) ---
@app.route('/admin/login', methods=['GET', 'POST'])
def admin_login():
    if 'admin_id' in session:
        admin_user_session = User.query.get(session['admin_id'])
        if admin_user_session and admin_user_session.is_admin and admin_user_session.is_active: # Aktiflik kontrolü
            return redirect(url_for('admin_dashboard'))

    if request.method == 'GET' and 'admin_id_pending_verification' in session:
        session.pop('admin_id_pending_verification', None)
        session.pop('admin_verification_code', None)
        session.pop('admin_code_expiration_utc', None)
        session.pop('admin_email_for_code', None)
        flash('Doğrulama işlemi iptal edildi.', 'info')

    if request.method == 'POST':
        identifier = request.form.get('identifier')
        password = request.form.get('password')

        if not identifier or not password:
            flash('E-posta/Kullanıcı adı ve şifre gereklidir.', 'danger')
        else:
            user = User.query.filter(User.is_admin == True).filter(
                (User.email == identifier) | (User.username == identifier)
            ).first()

            if user and user.check_password(password):
                if not user.email_verified:
                    flash('Admin e-postanız doğrulanmamış. Lütfen sistem yöneticinizle iletişime geçin.', 'danger')
                    return render_template('admin/admin_login.html', title="Admin Girişi")

                if not user.is_active: # YENİ: Aktiflik kontrolü
                    flash('Hesabınız devre dışı bırakılmış. Bu hesapla giriş yapamazsınız.', 'danger')
                    current_app.logger.warning(f"Devre dışı bırakılmış admin hesabı ({user.username}) için giriş denemesi.")
                    return render_template('admin/admin_login.html', title="Admin Girişi")
                
                verification_code = generate_verification_code()
                expiration_minutes = current_app.config['ADMIN_2FA_CODE_EXPIRATION_MINUTES']
                
                session['admin_id_pending_verification'] = user.id
                session['admin_verification_code'] = verification_code
                session['admin_code_expiration_utc'] = (datetime.now(timezone.utc) + timedelta(minutes=expiration_minutes)).isoformat()
                session['admin_email_for_code'] = user.email

                try:
                    send_admin_login_code_email(user.email, user.username, verification_code)
                    flash(f'{html.escape(user.email)} adresine bir doğrulama kodu gönderildi. Lütfen kodu giriniz.', 'info')
                    return redirect(url_for('admin_verify_code'))
                except Exception as e:
                    current_app.logger.error(f"Admin giriş (2FA) kodu e-postası gönderilemedi {user.email}: {e}")
                    flash('Doğrulama kodu gönderilirken bir hata oluştu. Lütfen tekrar deneyin veya yönetici ile iletişime geçin.', 'danger')
            else:
                flash('Geçersiz yönetici kimlik bilgileri veya yetki yok.', 'danger')
    return render_template('admin/admin_login.html', title="Admin Girişi")

# ... (admin_verify_code ve admin_logout aynı kalacak) ...
@app.route('/admin/verify-code', methods=['GET', 'POST'])
def admin_verify_code():
    if 'admin_id' in session:
        return redirect(url_for('admin_dashboard'))
    
    if 'admin_id_pending_verification' not in session or \
       'admin_verification_code' not in session or \
       'admin_code_expiration_utc' not in session:
        flash('Doğrulama işlemi başlatılmamış veya oturum süresi dolmuş. Lütfen tekrar giriş yapın.', 'warning')
        return redirect(url_for('admin_login'))

    admin_email_for_display = session.get('admin_email_for_code', 'kayıtlı e-posta adresinize')

    if request.method == 'POST':
        submitted_code = request.form.get('verification_code', '').strip()
        
        if not submitted_code:
            flash('Lütfen doğrulama kodunu giriniz.', 'danger')
            return render_template('admin/admin_verify_code.html', title="Admin Kod Doğrulama", email=admin_email_for_display)

        stored_code = session.get('admin_verification_code')
        expiration_str = session.get('admin_code_expiration_utc')
        expiration_utc = datetime.fromisoformat(expiration_str)

        if datetime.now(timezone.utc) > expiration_utc:
            session.pop('admin_id_pending_verification', None)
            session.pop('admin_verification_code', None)
            session.pop('admin_code_expiration_utc', None)
            session.pop('admin_email_for_code', None)
            flash('Doğrulama kodunun süresi dolmuş. Lütfen tekrar giriş yapın.', 'danger')
            return redirect(url_for('admin_login'))

        if submitted_code == stored_code:
            admin_id_to_login = session['admin_id_pending_verification']
            
            # Kod doğrulandıktan sonra kullanıcıyı tekrar çekip aktif mi diye kontrol et (ek güvenlik katmanı)
            user_to_login = User.query.get(admin_id_to_login)
            if not user_to_login or not user_to_login.is_admin or not user_to_login.is_active:
                flash('Hesabınızla ilgili bir sorun oluştu veya hesap aktif değil.', 'danger')
                session.pop('admin_id_pending_verification', None)
                session.pop('admin_verification_code', None)
                session.pop('admin_code_expiration_utc', None)
                session.pop('admin_email_for_code', None)
                return redirect(url_for('admin_login'))

            session.pop('admin_id_pending_verification', None)
            session.pop('admin_verification_code', None)
            session.pop('admin_code_expiration_utc', None)
            session.pop('admin_email_for_code', None)

            session['admin_id'] = admin_id_to_login
            session.permanent = True
            app.permanent_session_lifetime = timedelta(days=7)

            flash('Başarıyla doğrulandı ve giriş yaptınız!', 'success')
            return redirect(url_for('admin_dashboard'))
        else:
            flash('Geçersiz doğrulama kodu. Lütfen tekrar deneyin.', 'danger')
    
    return render_template('admin/admin_verify_code.html', title="Admin Kod Doğrulama", email=admin_email_for_display)

@app.route('/admin/logout')
@admin_required
def admin_logout(current_admin_user):
    session.pop('admin_id_pending_verification', None)
    session.pop('admin_verification_code', None)
    session.pop('admin_code_expiration_utc', None)
    session.pop('admin_email_for_code', None)
    session.pop('admin_id', None)
    flash('Başarıyla çıkış yaptınız.', 'info')
    return redirect(url_for('admin_login'))


@app.route('/admin')
@app.route('/admin/dashboard')
@admin_required
def admin_dashboard(current_admin_user):
    pending_questions_count = Question.query.filter_by(status='pending').count()
    reported_questions_count = QuestionReport.query.filter_by(status='new').join(Question).filter(Question.status != 'rejected').count()
    
    total_users = User.query.count()
    active_users = User.query.filter_by(is_active=True).count() # Aktif kullanıcı sayısı
    now_utc = datetime.now(timezone.utc)
    active_premium_users = User.query.filter(
        User.is_premium == True,
        User.is_active == True, # Aktif premium kullanıcılar
        User.premium_expiration_date != None,
        User.premium_expiration_date > now_utc
    ).count()
    total_questions = Question.query.count()
    approved_questions = Question.query.filter_by(status='approved').count()
    total_premium_questions = Question.query.filter_by(is_premium_only=True, status='approved').count()

    top_5_users = User.query.filter(User.is_admin == False, User.is_active == True).order_by(User.score.desc()).limit(5).all() # Sadece aktif kullanıcılar
    top_5_users_data = [{'username': html.escape(u.username), 'score': u.score, 'is_premium': u.has_active_premium} for u in top_5_users]

    live_stats_summary = {
        'total_users': total_users,
        'active_users': active_users, # Aktif kullanıcı sayısı eklendi
        'active_premium_users': active_premium_users,
        'total_questions': total_questions,
        'approved_questions': approved_questions,
        'total_premium_questions': total_premium_questions,
        'top_5_users': top_5_users_data
    }

    return render_template('admin/admin_dashboard.html',
                           title="Admin Paneli", admin_user=current_admin_user,
                           pending_count=pending_questions_count,
                           reported_count=reported_questions_count,
                           live_stats=live_stats_summary)

# --- KULLANICI YÖNETİMİ ROTASI ---
@app.route('/admin/manage_users', methods=['GET'])
@admin_required
def admin_manage_users(current_admin_user):
    # Sadece ana admin bu sayfayı görebilir (link zaten gizli ama çift kontrol)
    if current_admin_user.username != 'mehmetalper04':
        flash('Bu sayfaya erişim yetkiniz yok.', 'danger')
        return redirect(url_for('admin_dashboard'))
        
    users = User.query.order_by(User.is_admin.desc(), User.is_active.desc(), User.username).all()
    return render_template('admin/manage_users.html',
                           title="Kullanıcı Yönetimi",
                           admin_user=current_admin_user,
                           users_list=users)

# --- YENİ ADMİN EKLEME ROTASI (Yetki Kontrollü) ---
@app.route('/admin/manage_users/add_admin', methods=['GET', 'POST'])
@admin_required
def admin_add_new_admin_user(current_admin_user):
    if current_admin_user.username != 'mehmetalper04': # Sadece ana admin ekleyebilir
        flash('Sadece ana admin yeni admin kullanıcıları ekleyebilir.', 'danger')
        return redirect(url_for('admin_dashboard'))

    if request.method == 'POST':
        username = request.form.get('username')
        email = request.form.get('email')
        password = request.form.get('password')

        if not username or not email or not password:
            flash('Kullanıcı adı, e-posta ve şifre alanları zorunludur.', 'danger')
        elif len(password) < 6:
            flash('Şifre en az 6 karakter olmalıdır.', 'danger')
        elif User.query.filter_by(username=username).first():
            flash(f"'{username}' kullanıcı adı zaten mevcut. Lütfen farklı bir kullanıcı adı seçin.", 'danger')
        elif User.query.filter_by(email=email).first():
            flash(f"'{email}' e-posta adresi zaten bir kullanıcı tarafından kullanılıyor.", 'danger')
        else:
            try:
                new_admin = User(
                    username=username,
                    email=email,
                    is_admin=True,
                    email_verified=True,
                    is_active=True # Yeni eklenen adminler varsayılan olarak aktif
                )
                new_admin.set_password(password)
                db.session.add(new_admin)
                db.session.commit()
                flash(f"'{username}' isimli yeni admin kullanıcısı başarıyla oluşturuldu.", 'success')
                return redirect(url_for('admin_manage_users')) # Kullanıcı yönetimi sayfasına yönlendir
            except Exception as e:
                db.session.rollback()
                flash(f"Yeni admin oluşturulurken bir veritabanı hatası oluştu: {str(e)}", 'danger')
                current_app.logger.error(f"Error creating new admin user via web: {e}")
    
    return render_template('admin/add_admin_user.html', 
                           title="Yeni Admin Ekle", 
                           admin_user=current_admin_user)

# --- KULLANICI AKTİFLİK DURUMUNU DEĞİŞTİRME ROTASI ---
@app.route('/admin/toggle_user_status/<int:user_to_toggle_id>', methods=['POST'])
@admin_required
def admin_toggle_user_status(current_admin_user, user_to_toggle_id):
    if current_admin_user.username != 'mehmetalper04': # Sadece ana admin bu işlemi yapabilir
        flash('Kullanıcı durumunu sadece ana admin değiştirebilir.', 'danger')
        return redirect(url_for('admin_manage_users'))

    if current_admin_user.id == user_to_toggle_id: # Ana admin kendini devre dışı bırakamaz/aktifleştiremez
        flash("Kendi aktiflik durumunuzu bu arayüzden değiştiremezsiniz!", 'danger')
        return redirect(url_for('admin_manage_users'))

    user_to_toggle = User.query.get_or_404(user_to_toggle_id)

    if user_to_toggle.username == 'mehmetalper04': # Ana adminin durumu değiştirilemez
        flash(f"Ana admin kullanıcısının ('{user_to_toggle.username}') durumu değiştirilemez!", 'warning')
        return redirect(url_for('admin_manage_users'))

    try:
        user_to_toggle.is_active = not user_to_toggle.is_active
        action_taken = "aktifleştirildi" if user_to_toggle.is_active else "devre dışı bırakıldı"
        
        # Eğer bir admin devre dışı bırakılıyorsa ve tekrar aktif edilirse, admin olarak kalır.
        # Adminlik yetkisini almak için is_admin=False da ayarlanabilir.
        # if not user_to_toggle.is_active and user_to_toggle.is_admin:
        # user_to_toggle.is_admin = False # Örneğin, devre dışı bırakılan adminin yetkilerini de al
        # flash("Kullanıcının admin yetkileri de kaldırıldı.", "info")


        db.session.commit()
        flash(f"'{user_to_toggle.username}' kullanıcısı başarıyla {action_taken}.", 'success')
    except Exception as e:
        db.session.rollback()
        flash(f"Kullanıcı durumu değiştirilirken bir hata oluştu: {str(e)}", 'danger')
        current_app.logger.error(f"Error toggling status for user {user_to_toggle_id}: {e}")
    
    return redirect(url_for('admin_manage_users'))

# ... (diğer admin soru/rapor rotaları aynı kalacak) ...
@app.route('/admin/questions/pending')
@admin_required
def admin_pending_questions(current_admin_user):
    page = request.args.get('page', 1, type=int); per_page = 10
    pending_q_pagination = Question.query.filter_by(status='pending').order_by(Question.created_at.desc()).paginate(page=page, per_page=per_page, error_out=False)
    return render_template('admin/admin_pending_questions.html', title="Onay Bekleyen Sorular", admin_user=current_admin_user, questions_pagination=pending_q_pagination)

@app.route('/admin/questions/reported')
@admin_required
def admin_reported_questions(current_admin_user):
    page = request.args.get('page', 1, type=int); per_page = 10
    reported_q_pagination = QuestionReport.query.join(Question, QuestionReport.question_id == Question.id)\
        .filter(QuestionReport.status == 'new', Question.status != 'rejected')\
        .order_by(QuestionReport.created_at.desc())\
        .paginate(page=page, per_page=per_page, error_out=False)
    return render_template('admin/admin_reported_questions.html', title="Raporlanan Sorular", admin_user=current_admin_user, reports_pagination=reported_q_pagination)

@app.route('/admin/question/<int:question_id>/edit', methods=['GET', 'POST'])
@admin_required
def admin_edit_question(current_admin_user, question_id):
    question = Question.query.get_or_404(question_id)
    courses = Course.query.order_by(Course.name).all()
    if request.method == 'POST':
        question.text = request.form.get('text', question.text)
        question.course_id = request.form.get('course_id', question.course_id, type=int)
        question.option_a = request.form.get('option_a', question.option_a)
        question.option_b = request.form.get('option_b', question.option_b)
        question.option_c = request.form.get('option_c', question.option_c)
        question.option_d = request.form.get('option_d', question.option_d)
        question.option_e = request.form.get('option_e', question.option_e)
        question.correct_option = request.form.get('correct_option', question.correct_option).upper()
        new_status = request.form.get('status', question.status)
        question.is_premium_only = request.form.get('is_premium_only') == 'on'

        if new_status not in ['pending', 'approved', 'rejected', 'reported']: flash('Geçersiz soru durumu seçildi.', 'danger')
        else: question.status = new_status
        question.updated_at = datetime.utcnow()

        if 'image' in request.files:
            file = request.files['image']
            if file and file.filename != '' and allowed_file(file.filename):
                if question.image_filename:
                    old_image_path = os.path.join(current_app.config['UPLOAD_FOLDER'], question.image_filename)
                    if os.path.exists(old_image_path):
                        try: os.remove(old_image_path)
                        except OSError as e: current_app.logger.error(f"Eski resim silinemedi: {e}")
                filename = secure_filename(f"{uuid.uuid4()}_{file.filename}")
                file.save(os.path.join(current_app.config['UPLOAD_FOLDER'], filename)); question.image_filename = filename
            elif file.filename != '': flash('İzin verilmeyen dosya türü. Resim güncellenmedi.', 'warning')
        elif request.form.get('remove_image') == '1' and question.image_filename:
             old_image_path = os.path.join(current_app.config['UPLOAD_FOLDER'], question.image_filename)
             if os.path.exists(old_image_path):
                try: 
                    os.remove(old_image_path)
                    question.image_filename = None
                    current_app.logger.info(f"Resim kaldırıldı: {old_image_path}")
                except OSError as e: current_app.logger.error(f"Resim kaldırılırken hata: {e}")

        try:
            db.session.commit(); flash(f'Soru ID {question.id} güncellendi.', 'success')
            if question.status == 'approved':
                reports = QuestionReport.query.filter_by(question_id=question.id, status='new').all()
                for report in reports: report.status = 'resolved'
                db.session.commit()
            
            redirect_source = request.form.get('redirect_source') # Formdan al
            if redirect_source == 'pending':
                return redirect(url_for('admin_pending_questions'))
            elif redirect_source == 'reported':
                 return redirect(url_for('admin_reported_questions'))
            return redirect(url_for('admin_edit_question', question_id=question.id))
        except Exception as e: db.session.rollback(); flash(f'Soru güncellenirken hata: {str(e)}', 'danger')
    
    redirect_source_get = request.args.get('source')
    return render_template('admin/admin_edit_question.html', 
                           title=f"Soru Düzenle: ID {question.id}", 
                           admin_user=current_admin_user, 
                           question=question, 
                           courses=courses,
                           redirect_source=redirect_source_get)


@app.route('/admin/question/<int:question_id>/approve', methods=['POST'])
@admin_required
def admin_approve_question(current_admin_user, question_id):
    question = Question.query.get_or_404(question_id)
    redirect_source = request.form.get('source') # Geldiği yeri formdan al

    if question.status in ['pending', 'reported']:
        question.status = 'approved'; question.updated_at = datetime.utcnow()
        reports = QuestionReport.query.filter_by(question_id=question.id, status='new').all()
        for report in reports: report.status = 'resolved'
        db.session.commit(); flash(f'Soru ID {question.id} onaylandı ve ilişkili raporlar çözüldü.', 'success')
    else: flash(f'Soru ID {question.id} zaten onaylanmış veya farklı bir durumda.', 'warning')
    
    if redirect_source == 'pending':
        return redirect(url_for('admin_pending_questions'))
    elif redirect_source == 'reported':
        return redirect(url_for('admin_reported_questions'))
    elif redirect_source == 'edit':
        return redirect(url_for('admin_edit_question', question_id=question_id, source=redirect_source))
    return redirect(request.referrer or url_for('admin_dashboard'))


@app.route('/admin/question/<int:question_id>/reject', methods=['POST'])
@admin_required
def admin_reject_question(current_admin_user, question_id):
    question = Question.query.get_or_404(question_id)
    redirect_source = request.form.get('source')

    if question.status in ['pending', 'reported']:
        question.status = 'rejected'; question.updated_at = datetime.utcnow()
        reports = QuestionReport.query.filter_by(question_id=question.id, status='new').all()
        for report in reports: report.status = 'dismissed'
        db.session.commit(); flash(f'Soru ID {question.id} reddedildi ve ilişkili raporlar kapatıldı.', 'info')
    else: flash(f'Soru ID {question.id} zaten işlenmiş veya farklı bir durumda.', 'warning')

    if redirect_source == 'pending':
        return redirect(url_for('admin_pending_questions'))
    elif redirect_source == 'reported':
        return redirect(url_for('admin_reported_questions'))
    # Eğer düzenleme sayfasından geliyorsa ve reddediliyorsa, muhtemelen o sayfanın yenilenmesi yerine
    # ilgili listeye (onay bekleyenler veya raporlananlar) dönmek daha mantıklı olabilir.
    # Şimdilik dashboard'a yönlendirelim veya geldiği listeye.
    return redirect(request.referrer or url_for('admin_dashboard'))


@app.route('/admin/report/<int:report_id>/dismiss', methods=['POST'])
@admin_required
def admin_dismiss_report(current_admin_user, report_id):
    report = QuestionReport.query.get_or_404(report_id)
    if report.status in ['new', 'reviewed']:
        report.status = 'dismissed'
        question = report.question # Question objesine erişim
        if question: # Soru varsa (normalde olmalı)
            other_new_reports_for_same_question = QuestionReport.query.filter(
                QuestionReport.question_id == report.question_id,
                QuestionReport.status == 'new',
                QuestionReport.id != report.id
            ).count()
            
            if other_new_reports_for_same_question == 0 and question.status == 'reported':
                question.status = 'approved' # Başka rapor yoksa soruyu onayla
                question.updated_at = datetime.utcnow()
                flash(f'Rapor ID {report.id} kapatıldı. Soru ID {question.id} için başka yeni rapor kalmadığından soru onaylandı.', 'success')
            else:
                flash(f'Rapor ID {report.id} kapatıldı.', 'success')
        else: # Soru bir şekilde silinmişse veya yoksa
             flash(f'Rapor ID {report.id} kapatıldı, ancak ilişkili soru bulunamadı.', 'warning')
        db.session.commit()
    else: flash(f'Rapor ID {report.id} zaten işlenmiş veya farklı bir durumda.', 'warning')
    return redirect(url_for('admin_reported_questions'))


@app.route('/admin/question/<int:question_id>/delete', methods=['POST'])
@admin_required
def admin_delete_question(current_admin_user, question_id):
    question = Question.query.get_or_404(question_id)
    # İlişkili raporları ve cevapları sil
    QuestionReport.query.filter_by(question_id=question.id).delete(synchronize_session=False)
    UserAnswer.query.filter_by(question_id=question.id).delete(synchronize_session=False)
    
    if question.image_filename:
        image_path = os.path.join(current_app.config['UPLOAD_FOLDER'], question.image_filename)
        if os.path.exists(image_path):
            try: 
                os.remove(image_path)
                current_app.logger.info(f"Soru silinirken resim dosyası da silindi: {image_path}")
            except OSError as e: 
                current_app.logger.error(f"Soru silinirken resim dosyası silinirken hata: {e}")
                flash(f"Soru silindi ancak resim dosyası ({question.image_filename}) silinirken bir hata oluştu.", "warning")

    db.session.delete(question)
    db.session.commit()
    flash(f'Soru ID {question.id} ve ilişkili tüm veriler (raporlar, cevaplar, resim) başarıyla silindi.', 'success')
    
    prev_page = request.referrer
    if prev_page:
        if 'pending' in prev_page: return redirect(url_for('admin_pending_questions'))
        if 'reported' in prev_page: return redirect(url_for('admin_reported_questions'))
        if f'question/{question_id}/edit' in prev_page: return redirect(url_for('admin_dashboard')) 
    return redirect(url_for('admin_dashboard'))

# --- Context Processors ---
@app.context_processor
def inject_global_vars(): # Fonksiyon adı daha genel olabilir
    context = {
        'now': datetime.now(timezone.utc), # now_utc yerine now kullanıldı, önceki gibi
        'app_name': current_app.config['APP_NAME'], 
        'config': current_app.config, # config objesinin tamamını göndermek yerine spesifik değerler gönderilebilir
        'admin_user': None,
        'pending_count': 0,
        'reported_count': 0
    }
    if 'admin_id' in session:
        admin_user_ctx = User.query.get(session['admin_id'])
        if admin_user_ctx and admin_user_ctx.is_admin and admin_user_ctx.is_active: # Aktiflik kontrolü
            context['admin_user'] = admin_user_ctx
            context['pending_count'] = Question.query.filter_by(status='pending').count()
            context['reported_count'] = QuestionReport.query.filter_by(status='new').join(Question).filter(Question.status != 'rejected').count()
    return context

@app.context_processor
def inject_global_vars(): # Fonksiyon adı aynı kalabilir veya inject_template_globals yapabilirsiniz
    current_time_utc_aware = datetime.now(timezone.utc) # Zamanı bir kere alalım
    context = {
        'now': current_time_utc_aware,      # {{ now.year }} gibi kullanımlar için
        'now_utc': current_time_utc_aware,  # 'now_utc' is undefined hatasını gidermek için EKLENDİ
        'app_name': current_app.config['APP_NAME'],
        'config': current_app.config,
        'admin_user': None,
        'pending_count': 0,
        'reported_count': 0
    }
    if 'admin_id' in session:
        admin_user_ctx = User.query.get(session['admin_id'])
        # admin_user_ctx'nin None olmadığını ve aktif bir admin olduğunu kontrol et
        if admin_user_ctx and admin_user_ctx.is_admin and admin_user_ctx.is_active:
            context['admin_user'] = admin_user_ctx
            # pending_count ve reported_count sadece admin_user varsa anlamlı olabilir,
            # veya her zaman hesaplanabilir. Mevcut mantık admin_user varsa hesaplıyor.
            context['pending_count'] = Question.query.filter_by(status='pending').count()
            context['reported_count'] = QuestionReport.query.filter_by(status='new').join(Question).filter(Question.status != 'rejected').count()
    return context

@app.context_processor   
def utility_processor():
    def get_course_name(course_id): 
        course = Course.query.get(course_id)
        return html.escape(course.name) if course else "Bilinmiyor"
    def get_user_username(user_id): 
        user = User.query.get(user_id)
        return html.escape(user.username) if user else "Bilinmiyor"
    return dict(get_course_name=get_course_name, get_user_username=get_user_username)

# --- Initial Data Setup ---
def add_initial_data():
    with app.app_context():
        db.create_all()

        new_admin_email = 'mehmetalperkocaoglu@gmail.com'
        new_admin_username = 'mehmetalper04'
        # Kullanıcıyı bul veya oluştur (is_active default True olacak)
        admin_user = User.query.filter_by(username=new_admin_username).first()
        if not admin_user:
            admin_user = User.query.filter_by(email=new_admin_email).first()
        
        if not admin_user:
            admin_user = User(
                username=new_admin_username,
                email=new_admin_email,
                is_admin=True,
                email_verified=True,
                is_active=True # Varsayılan admin aktif olmalı
            )
            admin_user.set_password('adminpass')
            db.session.add(admin_user)
            print(f"Varsayılan admin kullanıcısı oluşturuldu: {new_admin_username} / adminpass (e-posta: {new_admin_email})")
        else:
            # Varolan adminin is_admin ve is_active durumlarını güncelle (opsiyonel)
            admin_user.is_admin = True
            admin_user.is_active = True
            admin_user.email_verified = True # Emin olmak için
            print(f"Varsayılan admin kullanıcısı ({new_admin_username}) zaten mevcut, durumu güncellendi (admin, aktif).")


        courses_data = ["Türkçe", "Matematik", "Fizik", "Kimya", "Biyoloji", "Tarih", "Coğrafya", "Felsefe", "Din Kültürü ve Ahlak Bilgisi"] # Uzun isim kullanıldı
        for course_name in courses_data:
            if not Course.query.filter_by(name=course_name).first():
                db.session.add(Course(name=course_name))
        
        math_course = Course.query.filter_by(name="Matematik").first()
        admin_user_for_q = User.query.filter_by(username=new_admin_username).first() # Doğru admini al

        if math_course and admin_user_for_q:
            if not Question.query.filter(Question.course_id == math_course.id, Question.text == "İki artı iki kaçtır?").first(): #
                q1 = Question(course_id=math_course.id, text="İki artı iki kaçtır?", option_a="3",option_b="4",option_c="5",option_d="6",option_e="7", correct_option="B", status='approved', submitted_by_user_id=admin_user_for_q.id, is_premium_only=False)
                db.session.add(q1)
            if not Question.query.filter(Question.course_id == math_course.id, Question.text == "x^2 - 4 = 0 ise x'in pozitif değeri kaçtır? (Premium)").first(): #
                q_premium = Question(course_id=math_course.id, text="x^2 - 4 = 0 ise x'in pozitif değeri kaçtır? (Premium)", option_a="1",option_b="2",option_c="-2",option_d="4",option_e="0", correct_option="B", status='approved', submitted_by_user_id=admin_user_for_q.id, is_premium_only=True)
                db.session.add(q_premium)
        
        try:
            db.session.commit()
            print("Veritabanı ilk verileri eklendi/güncellendi.")
        except Exception as e:
            db.session.rollback()
            print(f"İlk veri eklenirken/güncellenirken hata oluştu: {e}")


# --- Main Execution ---
if __name__ == '__main__':
    with app.app_context():
        add_initial_data() 
    app.run(debug=True, host='0.0.0.0', port=5000)

# --- Flask CLI Commands ---
@app.cli.command("init-db")
def init_db_command():
    """Veritabanını başlatır ve varsayılan verileri ekler."""
    add_initial_data()

@app.cli.command("create-admin")
@click.argument("username")
@click.argument("email")
@click.argument("password")
def create_admin_command(username, email, password):
    """Yeni bir admin kullanıcısı oluşturur."""
    if User.query.filter((User.username == username) | (User.email == email)).first():
        click.echo(f"Hata: Kullanıcı adı '{username}' veya e-posta '{email}' zaten mevcut.", err=True); return
    if len(password) < 6:
        click.echo("Hata: Şifre en az 6 karakter olmalıdır.", err=True); return
    admin_user_cli = User(username=username, email=email, is_admin=True, email_verified=True, is_active=True) # Yeni adminler aktif başlar
    admin_user_cli.set_password(password)
    try:
        db.session.add(admin_user_cli); db.session.commit()
        click.echo(f"Admin kullanıcısı '{username}' başarıyla oluşturuldu.")
    except Exception as e:
        db.session.rollback()
        click.echo(f"Admin kullanıcısı oluşturulurken hata: {str(e)}", err=True)

@app.cli.command("list-routes")
def list_routes():
    """Uygulamadaki tüm URL rotalarını listeler."""
    import urllib
    output = []
    for rule in app.url_map.iter_rules():
        options = {}
        for arg in rule.arguments:
            options[arg] = "[{0}]".format(arg)
        methods = ','.join(rule.methods)
        url = url_for(rule.endpoint, **options)
        line = urllib.parse.unquote("{:50s} {:20s} {}".format(rule.endpoint, methods, url))
        output.append(line)
    for line in sorted(output): # Sıralı göstermek için
        click.echo(line)