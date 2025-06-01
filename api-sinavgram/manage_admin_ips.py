import customtkinter as ctk
import hashlib
import os
import tkinter.messagebox as messagebox # Standart Tkinter messagebox'ı kullanabiliriz

# Admin IP'leri dosyasının konfigürasyonu
# Bu script'in proje kök dizininde olduğu ve 'instance' adlı bir alt klasör kullanıldığı varsayılır.
BASE_DIR = os.path.abspath(os.path.dirname(__file__))
INSTANCE_FOLDER_PATH = os.path.join(BASE_DIR, 'instance')
ADMIN_IPS_FILE = os.path.join(INSTANCE_FOLDER_PATH, "admin_ips.txt")

# --- Yardımcı Fonksiyonlar (önceki CLI script'inden) ---
def hash_ip(ip_address: str) -> str:
    """Verilen IP adresini SHA256 ile hashler."""
    return hashlib.sha256(ip_address.encode('utf-8')).hexdigest()

def load_ips_from_file() -> dict:
    """IP'leri dosyadan yükler. {isim: hashlenmis_ip} formatında bir sözlük döndürür."""
    ips = {}
    # instance klasörünün var olduğundan emin ol (okuma için de)
    if not os.path.exists(INSTANCE_FOLDER_PATH):
        try:
            os.makedirs(INSTANCE_FOLDER_PATH)
        except OSError as e:
            print(f"Uyarı: Instance klasörü ({INSTANCE_FOLDER_PATH}) oluşturulamadı: {e}")
            # Bu durumda dosya da bulunamayacaktır.

    if os.path.exists(ADMIN_IPS_FILE):
        try:
            with open(ADMIN_IPS_FILE, 'r', encoding='utf-8') as f:
                for line in f:
                    line = line.strip()
                    if line and ':' in line:
                        try:
                            name, hashed_ip_val = line.split(':', 1)
                            ips[name.strip()] = hashed_ip_val.strip()
                        except ValueError:
                            print(f"Uyarı: {ADMIN_IPS_FILE} dosyasında hatalı formatlı satır: '{line}'")
        except Exception as e:
            messagebox.showerror("Dosya Okuma Hatası", f"IP listesi dosyası okunurken hata oluştu: {e}")
    return ips

def save_ips_to_file(ips_data: dict) -> bool:
    """IP verilerini dosyaya kaydeder. ips_data {isim: hashlenmis_ip} formatındadır."""
    if not os.path.exists(INSTANCE_FOLDER_PATH):
        try:
            os.makedirs(INSTANCE_FOLDER_PATH)
        except OSError as e:
            messagebox.showerror("Dizin Hatası", f"Instance klasörü ({INSTANCE_FOLDER_PATH}) oluşturulamadı: {e}")
            return False
    try:
        with open(ADMIN_IPS_FILE, 'w', encoding='utf-8') as f:
            for name, hashed_ip_val in sorted(ips_data.items()): # İsimlere göre sıralı kaydet
                f.write(f"{name}:{hashed_ip_val}\n")
        return True
    except Exception as e:
        messagebox.showerror("Dosya Yazma Hatası", f"IP listesi dosyasına yazılırken hata oluştu: {e}")
        return False

# --- GUI Uygulaması ---
class AdminIPManagerApp(ctk.CTk):
    def __init__(self):
        super().__init__()

        self.title("Admin IP Yöneticisi")
        self.geometry("700x600") # Pencere boyutu ayarlandı
        ctk.set_appearance_mode("System") # "System", "Dark", "Light"
        ctk.set_default_color_theme("blue") # "blue", "green", "dark-blue"

        self.ips_data = {} # {isim: hashlenmis_ip}

        # Ana Çerçeve
        main_frame = ctk.CTkFrame(self, corner_radius=10)
        main_frame.pack(pady=15, padx=15, fill="both", expand=True)

        # Giriş Alanları Çerçevesi
        input_frame = ctk.CTkFrame(main_frame)
        input_frame.pack(pady=10, padx=10, fill="x")

        ctk.CTkLabel(input_frame, text="İsim/Tanımlayıcı:").grid(row=0, column=0, padx=(0,5), pady=5, sticky="w")
        self.name_entry = ctk.CTkEntry(input_frame, placeholder_text="örn: Ofis_Statik_IP", width=250)
        self.name_entry.grid(row=0, column=1, padx=5, pady=5, sticky="ew")

        ctk.CTkLabel(input_frame, text="IP Adresi:").grid(row=1, column=0, padx=(0,5), pady=5, sticky="w")
        self.ip_entry = ctk.CTkEntry(input_frame, placeholder_text="örn: 203.0.113.45", width=250)
        self.ip_entry.grid(row=1, column=1, padx=5, pady=5, sticky="ew")

        self.add_button = ctk.CTkButton(input_frame, text="Yeni IP Ekle", command=self.add_ip_gui, height=35)
        self.add_button.grid(row=2, column=0, columnspan=2, padx=5, pady=(10,5), sticky="ew")
        
        input_frame.columnconfigure(1, weight=1) # Giriş alanının genişlemesini sağlar

        # Liste Çerçevesi
        list_container_frame = ctk.CTkFrame(main_frame)
        list_container_frame.pack(pady=10, padx=10, fill="both", expand=True)
        
        ctk.CTkLabel(list_container_frame, text="Mevcut İzinli IP'ler (İsim: Hash)", font=ctk.CTkFont(weight="bold")).pack(anchor="w", pady=(0,5))

        self.scrollable_list_frame = ctk.CTkScrollableFrame(list_container_frame, height=300)
        self.scrollable_list_frame.pack(fill="both", expand=True)

        # Durum ve Yenileme Butonu Çerçevesi
        status_refresh_frame = ctk.CTkFrame(main_frame)
        status_refresh_frame.pack(pady=(5,10), padx=10, fill="x")

        self.refresh_button = ctk.CTkButton(status_refresh_frame, text="Listeyi Yenile", command=self.refresh_list_gui, width=120)
        self.refresh_button.pack(side="left", padx=(0,10))
        
        self.status_label = ctk.CTkLabel(status_refresh_frame, text="Durum: Hazır", text_color="gray", anchor="e")
        self.status_label.pack(side="right", fill="x", expand=True)

        self.refresh_list_gui() # Başlangıçta listeyi yükle

    def update_status(self, message, success=True):
        color = "green" if success else "red"
        self.status_label.configure(text=f"Durum: {message}", text_color=color)
        self.after(7000, lambda: self.status_label.configure(text="Durum: Hazır", text_color="gray"))


    def add_ip_gui(self):
        name = self.name_entry.get().strip()
        ip = self.ip_entry.get().strip()

        if not name or not ip:
            messagebox.showerror("Giriş Hatası", "İsim ve IP adresi alanları boş bırakılamaz.")
            self.update_status("İsim ve IP boş olamaz!", success=False)
            return

        if name in self.ips_data:
            messagebox.showerror("İsim Hatası", f"'{name}' ismi zaten kullanılıyor. Lütfen farklı bir isim seçin.")
            self.update_status(f"'{name}' ismi mevcut!", success=False)
            return
        
        # Basit bir IP formatı kontrolü (isteğe bağlı)
        # import re
        # if not re.match(r"^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$", ip): # Sadece IPv4 için
        #     messagebox.showerror("IP Format Hatası", f"'{ip}' geçerli bir IP adresi formatında görünmüyor.")
        #     self.update_status("Geçersiz IP formatı!", success=False)
        #     return

        hashed_ip = hash_ip(ip)
        
        for existing_name, existing_hash in self.ips_data.items():
            if existing_hash == hashed_ip:
                if messagebox.askyesno("IP Tekrarı Uyarısı", f"Bu IP ({ip}) zaten '{existing_name}' ismiyle kayıtlı.\n'{name}' ismiyle de eklemek istiyor musunuz?"):
                    pass # Kullanıcı onayladı, devam et
                else:
                    self.update_status("Ekleme iptal edildi (IP tekrarı).", success=False)
                    return # Kullanıcı iptal etti
                break 

        self.ips_data[name] = hashed_ip
        if save_ips_to_file(self.ips_data):
            self.update_status(f"'{name}' ({ip}) eklendi.", success=True)
            self.name_entry.delete(0, ctk.END)
            self.ip_entry.delete(0, ctk.END)
            self.refresh_list_gui()
        else:
            self.update_status("Dosyaya kaydetme başarısız!", success=False)
            self.ips_data = load_ips_from_file() # Başarısız olursa dosyadan geri yükle

    def remove_ip_gui(self, name_to_remove: str):
        if name_to_remove in self.ips_data:
            if messagebox.askyesno("Kaldırma Onayı", f"'{name_to_remove}' isimli IP kaydını silmek istediğinizden emin misiniz?"):
                self.ips_data.pop(name_to_remove)
                if save_ips_to_file(self.ips_data):
                    self.update_status(f"'{name_to_remove}' kaldırıldı.", success=True)
                    self.refresh_list_gui()
                else:
                    self.update_status("Dosyaya kaydetme başarısız (kaldırma)!", success=False)
                    self.ips_data = load_ips_from_file() # Başarısız olursa dosyadan geri yükle
        else:
            messagebox.showerror("Hata", f"'{name_to_remove}' ismi listede bulunamadı.")
            self.update_status(f"'{name_to_remove}' bulunamadı!", success=False)

    def refresh_list_gui(self):
        self.ips_data = load_ips_from_file()
        
        # Scrollable frame içindeki eski widget'ları temizle
        for widget in self.scrollable_list_frame.winfo_children():
            widget.destroy()

        if not self.ips_data:
            ctk.CTkLabel(self.scrollable_list_frame, text="Kayıtlı IP bulunmuyor.").pack(pady=10, padx=10)
            return

        for name, hashed_ip in sorted(self.ips_data.items()):
            item_frame = ctk.CTkFrame(self.scrollable_list_frame) # Her satır için bir çerçeve
            item_frame.pack(fill="x", pady=(2,3), padx=2)

            label_text = f"{name}:  {hashed_ip}" # Hash'in tamamını göster
            ip_label = ctk.CTkLabel(item_frame, text=label_text, anchor="w", wraplength=450) # Uzun hashler için satır sonu
            ip_label.pack(side="left", padx=(5,10), pady=5, expand=True, fill="x")
            
            remove_button = ctk.CTkButton(
                item_frame, 
                text="Sil", 
                command=lambda n=name: self.remove_ip_gui(n), # Doğru ismi lambda ile yakala
                width=60,
                height=28,
                fg_color="#D32F2F", # Kırmızı tonu
                hover_color="#B71C1C"  # Koyu kırmızı
            )
            remove_button.pack(side="right", padx=5, pady=5)

if __name__ == "__main__":
    # Uygulama başlamadan önce instance klasörünün var olduğundan emin olalım
    if not os.path.exists(INSTANCE_FOLDER_PATH):
        try:
            os.makedirs(INSTANCE_FOLDER_PATH)
            print(f"Oluşturulan dizin: {INSTANCE_FOLDER_PATH}")
        except OSError as e:
            # GUI başlamadan kritik bir hata olduğu için messagebox ile gösterilebilir
            messagebox.showerror("Başlangıç Hatası", f"Gerekli '{INSTANCE_FOLDER_PATH}' dizini oluşturulamadı: {e}\nUygulama düzgün çalışmayabilir.")
            # sys.exit(1) # İsteğe bağlı olarak program sonlandırılabilir
    
    app = AdminIPManagerApp()
    app.mainloop()