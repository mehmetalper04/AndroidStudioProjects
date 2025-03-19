<?php
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $image = $_FILES['question_image'];
    $imagePath = 'uploads/' . basename($image['name']);
    move_uploaded_file($image['tmp_name'], $imagePath);

    $option1 = $_POST['option_1'];
    $option2 = $_POST['option_2'];
    $option3 = $_POST['option_3'];
    $option4 = $_POST['option_4'];
    $option5 = $_POST['option_5'];
    $correctAnswer = $_POST['correct_answer'];

    $conn = new mysqli("localhost", "root", "", "quizapp");
    if ($conn->connect_error) {
        die("Connection failed: " . $conn->connect_error);
    }

    $query = "INSERT INTO Questions (image_url, correct_answer, option_1, option_2, option_3, option_4, option_5)
              VALUES ('$imagePath', '$correctAnswer', '$option1', '$option2', '$option3', '$option4', '$option5')";
    if ($conn->query($query) === TRUE) {
        echo "Question added successfully!";
    } else {
        echo "Error: " . $query . "<br>" . $conn->error;
    }
    $conn->close();
}
?>