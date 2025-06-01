<?php
require 'vendor/autoload.php';

use Firebase\Auth\Token\Exception\InvalidToken;
use Kreait\Firebase\Factory;

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $idToken = $_POST['idToken'];

    $factory = (new Factory)
        ->withServiceAccount('path/to/firebase-service-account.json');

    $auth = $factory->createAuth();

    try {
        $verifiedIdToken = $auth->verifyIdToken($idToken);
        $uid = $verifiedIdToken->claims()->get('sub');

        $conn = new mysqli("localhost", "root", "", "quizapp");
        if ($conn->connect_error) {
            die("Connection failed: " . $conn->connect_error);
        }

        $query = "SELECT * FROM Users WHERE user_id = '$uid'";
        $result = $conn->query($query);

        if ($result->num_rows === 0) {
            $email = $verifiedIdToken->claims()->get('email');
            $insertQuery = "INSERT INTO Users (user_id, email) VALUES ('$uid', '$email')";
            $conn->query($insertQuery);
        }

        echo json_encode(["success" => true, "message" => "Login successful."]);
    } catch (InvalidToken $e) {
        echo json_encode(["success" => false, "message" => "Invalid ID Token."]);
    } catch (\Exception $e) {
        echo json_encode(["success" => false, "message" => $e->getMessage()]);
    }
}
?>