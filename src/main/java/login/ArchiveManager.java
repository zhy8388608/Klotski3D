package login;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class ArchiveManager {
    static String archivePath = "D:/users.json"; //ArchiveManager.class.getResource("/").getPath().substring(1) + "users.json";

    // 读取用户存档
    public static UserArchive readUserArchive() {
        File file = new File(archivePath);
        if (!file.exists())
            return new UserArchive();
        ObjectMapper objectMapper = new ObjectMapper();
        if (file.exists()) {
            try {
                // 读取存储的哈希值
                String storedHash = readHashFromFile();
                // 计算当前存档文件的哈希值
                String currentHash = calculateFileHash(file);
                // 比较哈希值
                if (!currentHash.equals(storedHash)) {
                    LoginFrame.showDialog("存档文件可能已损坏，请检查！");
                }
                return objectMapper.readValue(file, UserArchive.class);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return new UserArchive();
    }

    // 写入用户存档
    public static void writeUserArchive(UserArchive archive) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(archivePath), archive);
            // 计算存档文件的哈希值
            String hash = calculateFileHash(new File(archivePath));
            // 将哈希值存储在另一个文件中
            saveHashToFile(hash);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // 计算文件的哈希值
    public static String calculateFileHash(File file) {
        try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(fileBytes);
            StringBuilder hexString = new StringBuilder(2 * encodedHash.length);
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 将哈希值存储在文件中
    public static void saveHashToFile(String hash) {
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(archivePath+".hash"), StandardCharsets.UTF_8)) {
            writer.write(hash);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 从文件中读取哈希值
    public static String readHashFromFile() {
        File hashFile = new File(archivePath + ".hash");
        if (hashFile.exists()) {
            try {
                return Files.readString(hashFile.toPath(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    // 检查用户是否存在
    public static boolean isUserExists(String username) {
        UserArchive archive = readUserArchive();
        List<User> users = archive.getUsers();
        for (User user : users) {
            if (user.getName().equals(username)) {
                return true;
            }
        }
        return false;
    }

    // 注册新用户
    public static void registerUser(String username, String password) {
        if (isUserExists(username)) {
            LoginFrame.showDialog("用户已存在");
            return;
        }
        UserArchive archive = readUserArchive();
        String hashedPassword = PasswordHasher.hashPassword(password);
        User newUser = new User();
        newUser.setName(username);
        newUser.setPassword(hashedPassword);
        newUser.setProgress(1);
        archive.getUsers().add(newUser);
        writeUserArchive(archive);
        LoginFrame.showDialog("注册成功");
    }
}
