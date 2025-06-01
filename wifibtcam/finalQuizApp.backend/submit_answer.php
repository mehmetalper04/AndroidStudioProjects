<?php
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $userId = $_POST['userId'];
    $questionId = $_POST['questionId'];
    $isCorrect = $_POST['isCorrect'];

    $conn = new mysqli("localhost", "root", "", "quizapp");
    if ($conn->connect_error) {
        die("Connection failed: " . $conn->connect_error);
    }

    if ($isCorrect === 'true') {
        $progressQuery = "SELECT answered_question_ids FROM UserProgress WHERE user_id = '$userId'";
        $progressResult = $conn->query($progressQuery);

        if ($progressResult->num_rows > 0) {
            $row = $progressResult->fetch_assoc();
            $answeredIds = $row['answered_question_ids'];
            $answeredIds .= (empty($answeredIds) ? "" : ",") . $questionId;

            $updateQuery = "UPDATE UserProgress SET answered_question_ids = '$answeredIds' WHERE user_id = '$userId'";
            $conn->query($updateQuery);
        } else {
            $insertQuery = "INSERT INTO UserProgress (user_id, answered_question_ids) VALUES ('$userId', '$questionId')";
            $conn->query($insertQuery);
        }
    }
    $conn->close();
}
?>
