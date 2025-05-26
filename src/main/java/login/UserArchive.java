package login;

import java.util.ArrayList;
import java.util.List;

// 用户存档数据模型
class UserArchive {
    private List<User> users;

    public UserArchive() {
        this.users = new ArrayList<>();
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}