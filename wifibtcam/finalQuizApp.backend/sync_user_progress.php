<?php
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $userId = $_POST['user_id'];
    $answeredIds = $_POST['answered_ids'];

    $conn = new mysqli("localhost", "root", "", "quizapp");
    if ($conn->connect_error) {
        die("Connection failed: " . $conn->connect_error);
    }

    $query = "SELECT answered_question_ids FROM UserProgress WHERE user_id = '$userId'";
    $result = $conn->query($query);

    if ($result->num_rows > 0) {
        $row = $result->fetch_assoc();
        $existingIds = $row['answered_question_ids'];

        $newIdsArray = explode(',', $answeredIds);
        $existingIdsArray = explode(',', $existingIds);
        $mergedIdsArray = array_unique(array_merge($existingIdsArray, $newIdsArray));
        $mergedIds = implode(',', $mergedIdsArray);

        $updateQuery = "UPDATE UserProgress SET answered_question_ids = '$mergedIds' WHERE user_id = '$userId'";
        if ($conn->query($updateQuery) === TRUE) {
            echo json_encode(["success" => true, "message" => "Progress updated successfully."]);
        } else {
            echo json_encode(["success" => false, "message" => "Failed to update progress."]);
        }
    } else {
        $insertQuery = "INSERT INTO UserProgress (user_id, answered_question_ids) VALUES ('$userId', '$answeredIds')";
        if ($conn->query($insertQuery) === TRUE) {
            echo json_encode(["success" => true, "message" => "Progress saved successfully."]);
        } else {
            echo json_encode(["success" => false, "message" => "Failed to save progress."]);
        }
    }

    $conn->close();
}
?>