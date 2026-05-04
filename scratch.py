import re

file_path = "src/main/java/com/ryujinsha/engine/GameGUI.java"
with open(file_path, "r") as f:
    content = f.read()

# Assignments first
content = content.replace("isGameOver = true;", "currentState = GameState.GAMEOVER;")
content = content.replace("isStruggling = true;", "currentState = GameState.STRUGGLING;")
content = content.replace("isStruggling = false;", "currentState = GameState.PLAYING;")
content = content.replace("isHidden = true;", "currentPosition = PlayerPosition.CABINET;")
content = content.replace("isHidden = false;", "currentPosition = PlayerPosition.BACK_ROOM;") 
content = content.replace("isLookingBack = false;", "currentPosition = PlayerPosition.FRONT_ROOM;")
content = content.replace("isLookingBack = true;", "currentPosition = PlayerPosition.BACK_ROOM;")
content = content.replace("isLookingBack = !isLookingBack;", "currentPosition = (currentPosition == PlayerPosition.FRONT_ROOM) ? PlayerPosition.BACK_ROOM : PlayerPosition.FRONT_ROOM;")

# Then conditions using regex to ensure whole word match
content = re.sub(r'\bisGameOver\b', '(currentState == GameState.GAMEOVER)', content)
content = re.sub(r'\bisStruggling\b', '(currentState == GameState.STRUGGLING)', content)
content = re.sub(r'\bisHidden\b', '(currentPosition == PlayerPosition.CABINET)', content)
content = re.sub(r'\bisLookingBack\b', '(currentPosition == PlayerPosition.BACK_ROOM)', content)

with open(file_path, "w") as f:
    f.write(content)
