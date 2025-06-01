<?php
if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $userId = $_GET['userId'];

    $conn = new mysqli("localhost", "root", "", "quizapp");
    if ($conn->connect_error) {
        die("Connection failed: " . $conn->connect_error);
    }

    $answeredQuery = "SELECT answered_question_ids FROM UserProgress WHERE user_id = '$userId'";
    $answeredResult = $conn->query($answeredQuery);
    $answeredIds = [];
    if ($answeredResult->num_rows > 0) {
        $row = $answeredResult->fetch_assoc();
        $answeredIds = explode(',', $row['answered_question_ids']);
    }

    $excludeIds = implode(',', array_map('intval', $answeredIds));
    $query = "SELECT * FROM Questions WHERE id NOT IN ($excludeIds) ORDER BY RAND() LIMIT 1";
    $result = $conn->query($query);

    if ($result->num_rows > 0) {
        echo json_encode($result->fetch_assoc());
    } else {
        echo json_encode(["message" => "No more questions available"]);
    }

    $conn->close();
}
?>