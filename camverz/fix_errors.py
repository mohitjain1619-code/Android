import os

base_dir = "C:/Users/mohit/Documents/AndroidStudioProjects - V15/camverz/app/src/main/java/com/mohitt/camverz/"

def replace_in_file(filename, old_str, new_str):
    path = os.path.join(base_dir, filename)
    if os.path.exists(path):
        with open(path, 'r', encoding='utf-8') as f:
            content = f.read()
        content = content.replace(old_str, new_str)
        with open(path, 'w', encoding='utf-8') as f:
            f.write(content)

# 1. CallActivity.java: api.getUserProfile(peerId) -> api.getUser(peerId)
replace_in_file("CallActivity.java", "api.getUserProfile", "api.getUser")

# 2. ChatActivity.java and MainScreenActivity.java: ChatCallActivity.class -> CallActivity.class
replace_in_file("ChatActivity.java", "ChatCallActivity.class", "CallActivity.class")
replace_in_file("MainScreenActivity.java", "ChatCallActivity.class", "CallActivity.class")

# 3. FloatingMenuManager.java: MainActivity.class -> MainScreenActivity.class
replace_in_file("FloatingMenuManager.java", "MainActivity.class", "MainScreenActivity.class")

# 4. InboxActivity.java and InboxAdapter.java: Conversation methods
replace_in_file("InboxActivity.java", "conversation.setUserName(", "conversation.setName(")
replace_in_file("InboxActivity.java", "conversation.setUserAvatar(", "conversation.setProfileImageUrl(")
replace_in_file("InboxAdapter.java", "conversation.getUserName()", "conversation.getName()")
replace_in_file("InboxAdapter.java", "conversation.getUserAvatar()", "conversation.getProfileImageUrl()")

# 5. MainScreenActivity.java and ProfileActivity.java: tokenManager.clear() -> tokenManager.clearToken()
replace_in_file("MainScreenActivity.java", "tokenManager.clear()", "tokenManager.clearToken()")
replace_in_file("ProfileActivity.java", "tokenManager.clear()", "tokenManager.clearToken()")

# 6. ProfileActivity.java: User getters and setters
user_path = os.path.join(base_dir, "User.java")
if os.path.exists(user_path):
    with open(user_path, 'r', encoding='utf-8') as f:
        user_content = f.read()
    if "int followersCount;" not in user_content:
        injection = """
    private int followersCount;
    private int followingCount;
    private boolean followedByMe;

    public int getFollowersCount() { return followersCount; }
    public void setFollowersCount(int followersCount) { this.followersCount = followersCount; }

    public int getFollowingCount() { return followingCount; }
    public void setFollowingCount(int followingCount) { this.followingCount = followingCount; }

    public boolean isFollowedByMe() { return followedByMe; }
    public void setFollowedByMe(boolean followedByMe) { this.followedByMe = followedByMe; }
"""
        user_content = user_content.replace("public class User {", "public class User {" + injection)
        with open(user_path, 'w', encoding='utf-8') as f:
            f.write(user_content)

# 7. ProfilePostAdapter.java: Post getters and setters
profile_post_adapter = os.path.join(base_dir, "ProfilePostAdapter.java")
if os.path.exists(profile_post_adapter):
    with open(profile_post_adapter, 'r', encoding='utf-8') as f:
        ppa_content = f.read()

    ppa_content = ppa_content.replace("post.getTimestamp()", "Long.parseLong(post.getCreatedAt())")
    ppa_content = ppa_content.replace("String.valueOf(post.getLikes().size())", "String.valueOf(post.getLikeCount())")
    ppa_content = ppa_content.replace("post.getLikes().contains(currentUserId)", "post.isLikedByMe()")
    ppa_content = ppa_content.replace("post.getPostId()", "post.getId()")
    ppa_content = ppa_content.replace("post.getLikes().remove(currentUserId);", "post.setLikeCount(post.getLikeCount() - 1); post.setLikedByMe(false);")
    ppa_content = ppa_content.replace("post.getLikes().add(currentUserId);", "post.setLikeCount(post.getLikeCount() + 1); post.setLikedByMe(true);")

    with open(profile_post_adapter, 'w', encoding='utf-8') as f:
        f.write(ppa_content)

# 8. VerificationActivity.java: Import JsonObject
verify_activity = os.path.join(base_dir, "VerificationActivity.java")
if os.path.exists(verify_activity):
    with open(verify_activity, 'r', encoding='utf-8') as f:
        va_content = f.read()

    if "import com.google.gson.JsonObject;" not in va_content:
        va_content = va_content.replace("import org.json.JSONObject;", "import org.json.JSONObject;\nimport com.google.gson.JsonObject;")
        with open(verify_activity, 'w', encoding='utf-8') as f:
            f.write(va_content)

print("Done fixing Java compilation errors!")
