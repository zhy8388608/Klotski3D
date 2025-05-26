package login;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

public class LoginFrame extends JFrame {

    private JButton loginBtn;
    private JButton registerBtn;
    private JLabel infoLabel;
    private Image backgroundImage;
    private Font titleFont;
    private Font otherFont;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton backBtn;
    private Timer animationTimer;
    private int targetY;
    private int startY;
    private int currentY;
    private int totalSteps = 20; // 动画总步数
    private int currentStep = 0; // 当前步数
    private JButton lastClickedButton; // 记录最后点击的按钮
    private JPanel selectionPanel;
    private JButton[] levelButtons;
    private User currentUser;
    private JLabel userLabel;
    private JLabel passLabel;
    private JButton showPasswordBtn;
    private Timer fadeInTimer;
    private Timer buttonFadeInTimer;
    private float alpha = 0.0f;
    private static final float FADE_IN_INCREMENT = 0.1f; // 增大透明度增量
    private int offsetX = 0;
    private int offsetY = 0;
    private int scaledWidth;
    private int scaledHeight;
    private int drawX;
    private int drawY;
    private JLabel titleLabel;
    private boolean isRestoringBackground = false; // 新增标志位
    private JPanel successPanel;  // 通关界面面板
    private JButton guestModeBtn;
    public static LoginFrame loginFrame;
    public static UserArchive userArchive;

    public LoginFrame() {
        loginFrame = this;

        this.setTitle("登录");
        this.setLayout(new BorderLayout());
        this.setSize(800, 500);
        this.setResizable(false);

        try {
            ImageIcon bgIcon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/login/background.jpg")));
            backgroundImage = bgIcon.getImage();
            calculateScaledSize();
        } catch (NullPointerException e) {
            System.err.println("背景图片加载失败");
            backgroundImage = null;
        }


        loadFont("/login/title_font.ttf", 40, Font.BOLD, true);

        loadFont("/login/content_font.ttf", 24, Font.BOLD, false);


        JPanel contentPane = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.drawImage(backgroundImage, drawX + offsetX, drawY + offsetY, scaledWidth, scaledHeight, this);
                }
            }
        };
        contentPane.setLayout(null);
        this.setContentPane(contentPane);


        titleLabel = new JLabel("KLOTSKI3D");
        if (titleFont != null) {
            titleLabel.setFont(titleFont.deriveFont(60f));
        } else {
            titleLabel.setFont(new Font("楷体", Font.BOLD, 60));
        }
        titleLabel.setForeground(new Color(255, 255, 255, 0));


        Dimension preferredSize = titleLabel.getPreferredSize();
        int titleWidth = preferredSize.width;
        int titleHeight = preferredSize.height;


        int centerX = (getWidth() - titleWidth) / 2;
        int centerY = (getHeight() - titleHeight) / 2;
        titleLabel.setBounds(centerX, centerY, titleWidth, titleHeight); // 使用动态计算的大小
        contentPane.add(titleLabel);

        initComponents();


        MouseAdapter mouseMoveAdapter = new MouseAdapter() {
            private double currentOffsetX = 0;
            private double currentOffsetY = 0;
            private final double dampingFactor = 0.2; // 阻尼系数，可根据需要调整
            private final double offsetFactorX = 0.1; // 减小左右方向的偏移系数
            private final double offsetFactorY = 0.1; // 减小上下方向的偏移系数
            private double targetOffsetX;
            private double targetOffsetY;
            private Timer smoothTransitionTimer;

            @Override
            public void mouseMoved(MouseEvent e) {
                if (isRestoringBackground) {
                    return;
                }
                int centerX = getWidth() / 2;
                int centerY = getHeight() / 2;
                int dx = e.getX() - centerX;
                int dy = e.getY() - centerY;

                targetOffsetX = -dx * offsetFactorX;
                targetOffsetY = -dy * offsetFactorY;

                if (smoothTransitionTimer != null && smoothTransitionTimer.isRunning()) {
                    return;
                }


                smoothTransitionTimer = new Timer(20, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {

                        currentOffsetX += (targetOffsetX - currentOffsetX) * dampingFactor;
                        currentOffsetY += (targetOffsetY - currentOffsetY) * dampingFactor;


                        int maxOffsetX = (scaledWidth - getWidth()) / 2;
                        offsetX = (int) Math.max(-maxOffsetX, Math.min(maxOffsetX, currentOffsetX));
                        int maxOffsetY = (scaledHeight - getHeight()) / 2;
                        offsetY = (int) Math.max(-maxOffsetY, Math.min(maxOffsetY, currentOffsetY));

                        contentPane.repaint();


                        if (Math.abs(currentOffsetX - targetOffsetX) < 1 && Math.abs(currentOffsetY - targetOffsetY) < 1) {
                            smoothTransitionTimer.stop();
                        }
                    }
                });
                smoothTransitionTimer.start();
            }

        };
        ;

        contentPane.addMouseMotionListener(mouseMoveAdapter);

        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);


        startTitleAnimation();
    }

    private void startTitleAnimation() {
        // 先黑屏
        alpha = 0.0f;
        setComponentAlpha(titleLabel, alpha);

        // 标题渐进显示
        fadeInTimer = new Timer(20, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                alpha += FADE_IN_INCREMENT;
                if (alpha >= 1.0f) {
                    alpha = 1.0f;
                    fadeInTimer.stop();

                    // 增加延迟，延长标题居中显示的时间
                    Timer delayTimer = new Timer(500, new ActionListener() { // 延迟1秒，可调整时间
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            // 开始标题移动动画
                            startTitleMoveAnimation();
                        }
                    });
                    delayTimer.setRepeats(false); // 只执行一次
                    delayTimer.start();
                }
                setComponentAlpha(titleLabel, alpha);
                repaint();
            }
        });
        fadeInTimer.start();
    }

    private static class EasingFunctions {
        // 弹性缓动函数 - 减小回弹效果
        public static double easeOutElastic(double t, double b, double c, double d) {
            double s = 1.70158;
            double p = d * 0.6; // 增加周期，减少回弹次数
            double a = c * 0.3; // 减小回弹幅度
            double dampingFactor = 0.3;

            if (t == 0) return b;
            if ((t /= d) == 1) return b + c;
            if (p == 0) p = d * 0.3;
            if (a < Math.abs(c)) {
                a = c;
                s = p / 4;
            } else {
                s = p / (2 * Math.PI) * Math.asin(c / a);
            }

            // 应用阻尼效果
            a *= Math.pow(dampingFactor, Math.floor(t));

            return a * Math.pow(2, -10 * t) * Math.sin((t * d - s) * (2 * Math.PI) / p) + c + b;
        }
    }

    private void startTitleMoveAnimation() {
        int startY = titleLabel.getY();
        int startX = titleLabel.getX();

        int targetY = 100;
        int targetX = 550;

        // 新增：定义开始和目标字体大小
        float startFontSize = 60;
        float targetFontSize = 40;

        // 计算背景的边界范围
        int minX = drawX + offsetX;
        int maxX = drawX + offsetX + scaledWidth - titleLabel.getWidth();
        int minY = drawY + offsetY;
        int maxY = drawY + offsetY + scaledHeight - titleLabel.getHeight();

        // 确保目标位置在背景范围内
        targetX = Math.max(minX, Math.min(targetX, maxX));
        targetY = Math.max(minY, Math.min(targetY, maxY));

        int totalSteps = 45; // 动画总步数
        final int[] currentStep = {0}; // 当前步数

        // 新增：保存原始字体以便后续派生
        Font originalFont = titleLabel.getFont();

        int finalTargetX = targetX;
        int finalTargetY = targetY;
        Timer moveTimer = new Timer(20, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentStep[0] < totalSteps) {
                    // 使用弹性缓动函数计算当前垂直位置
                    int currentY = (int) EasingFunctions.easeOutElastic(currentStep[0], startY, finalTargetY - startY, totalSteps);
                    // 使用弹性缓动函数计算当前水平位置
                    int currentX = (int) EasingFunctions.easeOutElastic(currentStep[0], startX, finalTargetX - startX, totalSteps);

                    // 新增：使用缓动函数计算当前字体大小
                    float currentFontSize = (float) EasingFunctions.easeOutElastic(
                            currentStep[0],
                            startFontSize,
                            targetFontSize - startFontSize,
                            totalSteps
                    );

                    // 新增：更新标题字体大小
                    titleLabel.setFont(originalFont.deriveFont(currentFontSize));

                    // 确保当前位置在背景范围内
                    currentX = Math.max(minX, Math.min(currentX, maxX));
                    currentY = Math.max(minY, Math.min(currentY, maxY));

                    titleLabel.setLocation(currentX, currentY);
                    currentStep[0]++;
                    repaint();
                } else {
                    titleLabel.setLocation(finalTargetX, finalTargetY);
                    // 确保最终字体大小准确
                    titleLabel.setFont(originalFont.deriveFont(targetFontSize));
                    ((Timer) e.getSource()).stop();
                    // 标题动画结束后开始登录按钮移动动画
                    startLoginButtonMoveAnimation();
                }
            }
        });
        moveTimer.start();
    }

    private void startLoginButtonMoveAnimation() {
        int startX = getWidth(); // 初始位置在窗口右侧外
        int targetX = 500; // 目标位置
        int totalSteps = 20; // 动画总步数
        final int[] currentStep = {0}; // 当前步数

        loginBtn.setLocation(startX, loginBtn.getY());
        loginBtn.setVisible(true);
        registerBtn.setVisible(true);
        infoLabel.setVisible(true);

        Timer buttonMoveTimer = new Timer(20, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentStep[0] < totalSteps) {
                    // 使用弹性缓动函数计算当前水平位置
                    int currentX = (int) EasingFunctions.easeOutElastic(currentStep[0], startX, targetX - startX, totalSteps);
                    loginBtn.setLocation(currentX, loginBtn.getY());
                    currentStep[0]++;
                    repaint();
                } else {
                    loginBtn.setLocation(targetX, loginBtn.getY());
                    ((Timer) e.getSource()).stop();
                    // 登录按钮移动结束后开始注册按钮的移动动画
                    startRegisterButtonMoveAnimation();
                }
            }
        });
        buttonMoveTimer.start();
    }

    private void startRegisterButtonMoveAnimation() {
        int originalX = registerBtn.getX();
        int startX = getWidth(); // 初始位置在窗口右侧外
        int targetX = 500; // 目标位置
        int totalSteps = 20; // 动画总步数
        final int[] currentStep = {0}; // 当前步数

        registerBtn.setLocation(startX, registerBtn.getY());

        Timer buttonMoveTimer = new Timer(20, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentStep[0] < totalSteps) {
                    // 使用弹性缓动函数计算当前水平位置
                    int currentX = (int) EasingFunctions.easeOutElastic(currentStep[0], startX, targetX - startX, totalSteps);
                    registerBtn.setLocation(currentX, registerBtn.getY());
                    currentStep[0]++;
                    repaint();
                } else {
                    registerBtn.setLocation(targetX, registerBtn.getY());
                    ((Timer) e.getSource()).stop();
                    // 注册按钮移动结束后开始其他按钮的淡入动画
                    startButtonFadeIn();
                }
            }
        });
        buttonMoveTimer.start();
    }

    private void startButtonFadeIn() {
        alpha = 0.0f;
        if (buttonFadeInTimer != null && buttonFadeInTimer.isRunning()) {
            buttonFadeInTimer.stop();
        }
        buttonFadeInTimer = new Timer(20, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                alpha += FADE_IN_INCREMENT;
                if (alpha >= 1.0f) {
                    alpha = 1.0f;
                    buttonFadeInTimer.stop();
                }
                setComponentAlpha(registerBtn, alpha);
                setComponentAlpha(infoLabel, alpha);
                repaint();
            }
        });
        buttonFadeInTimer.start();
    }

    private void calculateScaledSize() {
        int frameWidth = getWidth();
        int frameHeight = getHeight();
        int imageWidth = backgroundImage.getWidth(null);
        int imageHeight = backgroundImage.getHeight(null);

        // 计算宽度和高度的缩放比例
        double widthRatio = (double) frameWidth / imageWidth;
        double heightRatio = (double) frameHeight / imageHeight;

        // 选择较大的缩放比例，以确保背景图完全覆盖窗口
        double scaleRatio = Math.max(widthRatio, heightRatio);

        // 增加左右拉伸的比例，你可以根据需要调整这个值
        double stretchRatio = 1.2;
        widthRatio *= stretchRatio;
        scaleRatio = Math.max(widthRatio, heightRatio);

        // 计算缩放后的宽度和高度
        scaledWidth = (int) (imageWidth * scaleRatio);
        scaledHeight = (int) (imageHeight * scaleRatio);

        // 计算绘制背景图的起始位置，使其居中显示
        drawX = (frameWidth - scaledWidth) / 2;
        drawY = (frameHeight - scaledHeight) / 2;

        // 确保背景图左右两侧边框不会露出
        if (scaledWidth < frameWidth) {
            scaleRatio = (double) frameWidth / imageWidth;
            scaledWidth = frameWidth;
            scaledHeight = (int) (imageHeight * scaleRatio);
            drawY = (frameHeight - scaledHeight) / 2;
            drawX = 0;
        }
    }

    private void loadFont(String fontPath, float size, int style, boolean isTitleFont) {
        try {
            // 加载字体文件
            InputStream fontStream = getClass().getResourceAsStream(fontPath);
            if (fontStream != null) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                // 可以根据需要调整字体大小和样式
                font = font.deriveFont(style, size);
                // 注册字体到系统
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(font);
                if (isTitleFont) {
                    titleFont = font;
                } else {
                    otherFont = font;
                }
            } else {
                System.err.println("字体文件 " + fontPath + " 加载失败");
            }
        } catch (FontFormatException | IOException e) {
            System.err.println("字体 " + fontPath + " 加载失败: " + e.getMessage());
        }
    }

    private void initComponents() {
        // 半透明蒙版层
        JPanel overlay = new JPanel();
        overlay.setBounds(0, 0, 800, 500);
        overlay.setOpaque(false);
        overlay.setLayout(null);

        // 初始化 successPanel
        successPanel = new JPanel();
        successPanel.setBounds(0, 0, 800, 500);
        successPanel.setOpaque(false);
        successPanel.setLayout(null);
        successPanel.setVisible(false);
        this.getContentPane().add(successPanel);

        // 登录按钮
        loginBtn = createMinecraftButton("登录", new Point(getWidth(), 200), 250, 40);
        if (otherFont != null) {
            loginBtn.setFont(otherFont);
        }
        setComponentAlpha(loginBtn, 0f); // 初始透明度为0
        loginBtn.setVisible(false);
        overlay.add(loginBtn);

        // 注册按钮
        registerBtn = createMinecraftButton("注册", new Point(getWidth(), 260), 250, 40);
        if (otherFont != null) {
            registerBtn.setFont(otherFont);
        }
        setComponentAlpha(registerBtn, 0f); // 初始透明度为0
        registerBtn.setVisible(false);
        overlay.add(registerBtn);

        // 帮助信息
        infoLabel = new JLabel("<html><center><br><u style='color:#58a6ff;'>请点击此处 来获取更多帮助。</u></center>");
        if (otherFont != null) {
            infoLabel.setFont(otherFont.deriveFont(Font.PLAIN, 12));
        } else {
            infoLabel.setFont(new Font("楷体", Font.PLAIN, 12));
        }
        infoLabel.setForeground(new Color(34, 255, 0, 42));
        infoLabel.setBounds(500, 280, 250, 60);
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        infoLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // 使用绿色标注鼠标和键盘操作
                String helpMessage = "<html><body style='font-family:Arial; line-height:1.5;'>" +
                        "<h3 style='color:#333; margin-bottom:5px;'>操作指南</h3>" +
                        "<ul style='padding-left:20px; margin:0;'>" +
                        "<li><span style='color:green'>右键</span>按住拖动可旋转视角，<span style='color:green'>滚轮</span>控制缩放。</li>" +
                        "<li><span style='color:green'>单击</span>方块可以选中，选中后：</li>" +
                        "<ul style='padding-left:20px; margin:5px 0;'>" +
                        "<li><span style='color:green'>左键</span>按住拖动可以移动；</li>" +
                        "<li>按键盘 <span style='color:green'>WASD</span> 上左下右移动，<span style='color:green'>QE</span> 前后移动。</li>" +
                        "</ul>" +
                        "<li>选中后按住 <span style='color:green'>F</span> 可以仅透视当前选中的方块，按住 <span style='color:green'>F</span> 滚动<span style='color:green'>滚轮</span>可切换选中并透视下一个方块。</li>" +
                        "<li>按住 <span style='color:green'>R</span> 滚动<span style='color:green'>滚轮</span>可从当前视角方向对整体进行切片观察。</li>" +
                        "<li>按 <span style='color:green'>N</span> 重新开始，按 <span style='color:green'>Z</span> 撤销，按 <span style='color:green'>Y</span> 重做，按 <span style='color:green'>ESC</span> 退出。</li>" +
                        "<li>按 <span style='color:green'>H</span> 后长按 <span style='color:green'>Y</span> 提示。</li>" +
                        "</ul></body></html>";

                // 创建一个非模态对话框来显示帮助信息
                JDialog dialog = new JDialog(LoginFrame.this, "帮助信息", false);

                // 创建标签并应用与按钮相同的字体
                JLabel messageLabel = new JLabel(helpMessage);
                if (otherFont != null) {
                    messageLabel.setFont(otherFont.deriveFont(Font.PLAIN, 16)); // 使用 otherFont 并设置大小为 16
                } else {
                    messageLabel.setFont(new Font("Arial", Font.PLAIN, 16));
                }
                messageLabel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
                dialog.add(messageLabel);
                dialog.pack();
                dialog.setLocationRelativeTo(LoginFrame.this);
                dialog.setVisible(true);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                infoLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        });
        setComponentAlpha(infoLabel, 0f); // 初始透明度为0
        infoLabel.setVisible(false);
        overlay.add(infoLabel);

        // 返回按钮
        backBtn = createMinecraftButton("返回", new Point(500, 150), 250, 40);
        if (otherFont != null) {
            backBtn.setFont(otherFont);
        }
        backBtn.setVisible(false);
        overlay.add(backBtn);

        // 用户和密码界面
        userLabel = new JLabel("用户 :");
        if (otherFont != null) {
            userLabel.setFont(otherFont.deriveFont(Font.PLAIN, 18));
        } else {
            userLabel.setFont(new Font("微软雅黑", Font.PLAIN, 18));
        }
        userLabel.setForeground(Color.WHITE);
        userLabel.setBounds(500, 200, 80, 30);
        userLabel.setOpaque(false);
        overlay.add(userLabel);

        // 计算输入框的宽度和位置，使其右侧与返回按钮对齐
        int inputWidth = 200; // 输入框宽度
        int inputX = backBtn.getX() + backBtn.getWidth() - inputWidth;
        usernameField = new JTextField();
        usernameField.setBounds(inputX, 200, inputWidth, 30);
        styleTextField(usernameField);
        if (otherFont != null) {
            usernameField.setFont(otherFont.deriveFont(Font.PLAIN, 18));
        }
        usernameField.setOpaque(false);
        overlay.add(usernameField);

        passLabel = new JLabel("密码 :");
        if (otherFont != null) {
            passLabel.setFont(otherFont.deriveFont(Font.PLAIN, 18));
        } else {
            passLabel.setFont(new Font("微软雅黑", Font.PLAIN, 18));
        }
        passLabel.setForeground(Color.WHITE);
        passLabel.setBounds(500, 250, 80, 30);
        passLabel.setOpaque(false);
        overlay.add(passLabel);

        passwordField = new JPasswordField();
        passwordField.setBounds(inputX, 250, inputWidth - 30, 30); // 调整输入框宽度，为查看密码按钮留出空间
        styleTextField(passwordField);
        if (otherFont != null) {
            passwordField.setFont(otherFont.deriveFont(Font.PLAIN, 18));
        }
        passwordField.setOpaque(false);
        overlay.add(passwordField);

        // 查看密码按钮
        showPasswordBtn = new JButton("显示");
        showPasswordBtn.setBounds(inputX + inputWidth - 30, 250, 30, 30);
        showPasswordBtn.addActionListener(e -> {
            if (showPasswordBtn.getText().equals("显示")) {
                passwordField.setEchoChar((char) 0); // 显示明文
                showPasswordBtn.setText("隐藏");
            } else {
                passwordField.setEchoChar('*'); // 显示密文
                showPasswordBtn.setText("显示");
            }
        });
        showPasswordBtn.setOpaque(false);
        overlay.add(showPasswordBtn);

        // 初始时隐藏用户和密码界面
        userLabel.setVisible(false);
        usernameField.setVisible(false);
        passLabel.setVisible(false);
        passwordField.setVisible(false);
        showPasswordBtn.setVisible(false);

        // 游客模式按钮
        guestModeBtn = createMinecraftButton("游客模式", new Point(500, 345), 250, 40);
        if (otherFont != null) {
            guestModeBtn.setFont(otherFont);
        }
        guestModeBtn.setVisible(false);
        overlay.add(guestModeBtn);

        // 标志位，用于判断是否是第一次点击注册按钮
        final boolean[] isFirstRegisterClick = {true};

        // 按钮事件绑定
        loginBtn.addActionListener(e -> {
            lastClickedButton = loginBtn;
            if (userLabel.isVisible()) {
                handleMicrosoftLogin();
            } else {
                // 隐藏其他按钮
                registerBtn.setVisible(false);
                infoLabel.setVisible(false);
                // 显示用户和密码界面
                userLabel.setVisible(true);
                usernameField.setVisible(true);
                passLabel.setVisible(true);
                passwordField.setVisible(true);
                showPasswordBtn.setVisible(true);
                // 显示返回按钮
                backBtn.setVisible(true);
                // 显示游客模式按钮
                guestModeBtn.setVisible(true);
                // 强制重绘界面
                getContentPane().repaint();
                // 开始动画移动登录按钮到下方
                startAnimation(290);
                // 开始淡入动画
                startFadeInAnimation();
            }
        });

// 游客模式按钮事件绑定
        guestModeBtn.addActionListener(e -> {
            // 弹出提示框
            JOptionPane.showMessageDialog(this, "游客模式不会储存游戏进度", "提示", JOptionPane.INFORMATION_MESSAGE);
            userArchive = ArchiveManager.readUserArchive();
            currentUser = new User();
            currentUser.setName("Guest");
            showSelectionPanel();
        });

// 注册按钮事件绑定
        registerBtn.addActionListener(e -> {
            lastClickedButton = registerBtn;
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());

            // 显示用户和密码界面
            userLabel.setVisible(true);
            usernameField.setVisible(true);
            passLabel.setVisible(true);
            passwordField.setVisible(true);
            showPasswordBtn.setVisible(true);
            // 显示返回按钮
            backBtn.setVisible(true);

            // 隐藏其他按钮
            loginBtn.setVisible(false);  // 添加这行代码
            infoLabel.setVisible(false);

            // 如果是第一次点击注册按钮，开始动画移动注册按钮到下方
            if (isFirstRegisterClick[0]) {
                startAnimation(290);
                // 开始淡入动画
                startFadeInAnimation();
            }

            // 处理注册逻辑
            if (!username.isEmpty() && !password.isEmpty()) {
                ArchiveManager.registerUser(username, password);
            }

            isFirstRegisterClick[0] = false; // 更新标志变量
        });

        backBtn.addActionListener(e -> {
            // 显示其他按钮
            loginBtn.setVisible(true);
            registerBtn.setVisible(true);
            infoLabel.setVisible(true);
            // 隐藏用户和密码界面
            userLabel.setVisible(false);
            usernameField.setVisible(false);
            passLabel.setVisible(false);
            passwordField.setVisible(false);
            showPasswordBtn.setVisible(false);
            // 隐藏返回按钮
            backBtn.setVisible(false);
            // 根据之前点击的按钮恢复位置
            if (lastClickedButton == loginBtn) {
                startAnimation(200);
            } else if (lastClickedButton == registerBtn) {
                startAnimation(260);
            }
            // 隐藏游客模式按钮
            guestModeBtn.setVisible(false);

            // 重置 isFirstRegisterClick 标志位
            isFirstRegisterClick[0] = true;
        });

        backBtn.addActionListener(e -> {
            // 显示其他按钮
            loginBtn.setVisible(true);
            registerBtn.setVisible(true);
            infoLabel.setVisible(true);
            // 隐藏用户和密码界面
            userLabel.setVisible(false);
            usernameField.setVisible(false);
            passLabel.setVisible(false);
            passwordField.setVisible(false);
            showPasswordBtn.setVisible(false);
            // 隐藏返回按钮
            backBtn.setVisible(false);
            // 根据之前点击的按钮恢复位置
            if (lastClickedButton == loginBtn) {
                startAnimation(200);
            } else if (lastClickedButton == registerBtn) {
                startAnimation(260);
            }
            // 隐藏游客模式按钮
            guestModeBtn.setVisible(false);
        });

        // 添加蒙版层到内容面板
        this.getContentPane().add(overlay);

        // 初始化选关面板
        selectionPanel = new JPanel();
        selectionPanel.setBounds(0, 0, 800, 500);
        selectionPanel.setOpaque(false);
        selectionPanel.setLayout(null);
        selectionPanel.setVisible(false);
        this.getContentPane().add(selectionPanel);

        // 初始化关卡按钮
        levelButtons = new JButton[4]; // 修改数组大小为4
        for (int i = 0; i < 4; i++) {
            int levelIndex = i + 1;
            int buttonX = getWidth() - 200 - 100; // 按钮距离右侧边界20像素
            int buttonY = 100 + i * 80;
            // 修改按钮文本为 "关卡" + 关卡索引
            levelButtons[i] = createMinecraftButton("关卡 " + levelIndex, new Point(buttonX, buttonY), 200, 60);
            if (otherFont != null) {
                levelButtons[i].setFont(otherFont);
            }
            levelButtons[i].addActionListener(e -> {
                startLevel(levelIndex);
            });
            selectionPanel.add(levelButtons[i]);
        }

        // Add ranking button above level 1 button
        JButton rankingBtn = createMinecraftButton("用户排名", new Point(getWidth() - 200 - 100, 20), 200, 60);
        if (otherFont != null) {
            rankingBtn.setFont(otherFont);
        }
        rankingBtn.addActionListener(e -> showUserRanking());
        selectionPanel.add(rankingBtn);
    }


    private void showUserRanking() {
        SwingUtilities.invokeLater(() -> {
            // Create non-modal dialog
            UserRankingFrame rankingFrame = new UserRankingFrame(
                    (Frame) SwingUtilities.getWindowAncestor(LoginFrame.this),
                    false // Set to non-modal
            );
            rankingFrame.setVisible(true);
        });
    }

    private void styleTextField(JComponent field) {
        field.setBackground(new Color(36, 41, 46));
        field.setForeground(Color.WHITE);
        field.setBorder(BorderFactory.createLineBorder(new Color(85, 91, 98), 1));
        field.setFont(new Font("微软雅黑", Font.PLAIN, 14));
    }

    private JButton createMinecraftButton(String text, Point location, int width, int height) {
        JButton button = new JButton(text) {
            private final Color defaultColor = new Color(67, 133, 47, 128);
            private final Color hoverColor = new Color(87, 153, 67, 128);
            private final Color pressedColor = new Color(45, 55, 58, 255);

            @Override
            protected void paintComponent(Graphics g) {
                if (getModel().isPressed()) {
                    g.setColor(pressedColor);
                } else if (getModel().isRollover()) {
                    g.setColor(hoverColor);
                } else {
                    g.setColor(defaultColor);
                }
                g.fill3DRect(0, 0, getWidth(), getHeight(), true);

                g.setColor(Color.WHITE);
                FontMetrics fm = g.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                g.drawString(getText(), x, y);
            }

            @Override
            protected void paintBorder(Graphics g) {
                g.setColor(new Color(34, 255, 0, 42));
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            }
        };

        button.setBorder(BorderFactory.createEmptyBorder());
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBounds(location.x, location.y, width, height);
        button.setRolloverEnabled(true);

        // Set custom font if available
        if (otherFont != null) {
            button.setFont(otherFont.deriveFont(Font.PLAIN, 16f));
        } else {
            button.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        }

        return button;
    }

    // Modify the handleMicrosoftLogin method:
    private void handleMicrosoftLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "用户名和密码不能为空", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        userArchive = ArchiveManager.readUserArchive();
        List<User> users = userArchive.getUsers();
        boolean userFound = false;
        String hashedPassword = PasswordHasher.hashPassword(password);
        for (User user : users) {
            if (user.getName().equals(username) && user.getPassword().equals(hashedPassword)) {
                userFound = true;
                currentUser = user;
                break;
            }
        }

        if (userFound) {
            updateLevelButtons();
            showSelectionPanel();
        } else {
            showDialog("用户名或密码错误");
        }
    }

    private void showSelectionPanel() {
        // 隐藏登录界面的所有按钮和标签
        loginBtn.setVisible(false);
        registerBtn.setVisible(false);
        infoLabel.setVisible(false);
        userLabel.setVisible(false);
        usernameField.setVisible(false);
        passLabel.setVisible(false);
        passwordField.setVisible(false);
        showPasswordBtn.setVisible(false);
        backBtn.setVisible(false);
        // 隐藏游客模式按钮
        guestModeBtn.setVisible(false);

        // 将返回按钮透明度设置为0
        setComponentAlpha(backBtn, 0f);
        // 隐藏登录界面
        this.getContentPane().getComponent(0).setVisible(false);
        // 显示选关界面
        selectionPanel.setVisible(true);
    }

    private void startAnimation(int targetY) {
        this.targetY = targetY;
        this.startY = lastClickedButton.getY();
        this.currentY = startY;
        this.currentStep = 0;
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        animationTimer = new Timer(20, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentStep < totalSteps) {
                    // 使用缓动函数计算当前位置
                    currentY = (int) EasingFunctions.easeOutElastic(currentStep, startY, targetY - startY, totalSteps);
                    lastClickedButton.setLocation(lastClickedButton.getX(), currentY);
                    currentStep++;
                } else {
                    lastClickedButton.setLocation(lastClickedButton.getX(), targetY);
                    animationTimer.stop();
                }
            }
        });
        animationTimer.start();
    }

    public static void showDialog(String notice) {
        JOptionPane.showMessageDialog(loginFrame, notice, "提示", JOptionPane.INFORMATION_MESSAGE);
    }

    private void startFadeInAnimation() {
        alpha = 0.0f;
        if (fadeInTimer != null && fadeInTimer.isRunning()) {
            fadeInTimer.stop();
        }
        fadeInTimer = new Timer(20, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                alpha += FADE_IN_INCREMENT;
                if (alpha >= 1.0f) {
                    alpha = 1.0f;
                    fadeInTimer.stop();
                }
                setComponentAlpha(userLabel, alpha);
                setComponentAlpha(usernameField, alpha);
                setComponentAlpha(passLabel, alpha);
                setComponentAlpha(passwordField, alpha);
                setComponentAlpha(showPasswordBtn, alpha);
                setComponentAlpha(backBtn, alpha);
                repaint();
            }
        });
        fadeInTimer.start();
    }

    private void setComponentAlpha(JComponent component, float alpha) {
        Color color = component.getForeground();
        component.setForeground(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha * 255)));
        color = component.getBackground();
        component.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha * 255)));
    }

    // 添加必要的getter方法，以便LevelCompleteDialog可以访问相关属性
    public Image getBackgroundImage() {
        return backgroundImage;
    }

    public int getDrawX() {
        return drawX;
    }

    public int getDrawY() {
        return drawY;
    }

    public int getScaledWidth() {
        return scaledWidth;
    }

    public int getScaledHeight() {
        return scaledHeight;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        if (backgroundImage != null) {
            calculateScaledSize();
            repaint();
        }
    }


    void startLevel(int level) {
        System.out.println("Starting..." + level);
        if (currentUser.getName().equals("Guest"))
            System.out.println("不保存");
        Intermediary.load(currentUser, level);

    }

    // 更新关卡按钮状态
    public void updateLevelButtons() {
        for (int i = 0; i < 4; i++) {
            levelButtons[i].setEnabled(true);
        }
        for (int i = currentUser.getProgress(); i < 4; i++) {
            levelButtons[i].setEnabled(false);
        }
        for (int i = 0; i < currentUser.getProgress() - 1; i++) { // 修改循环次数为4
            Level level = currentUser.getLevels().get(i);
            if(level.getBestSteps()<=0)
                continue;
            String buttonText = "关卡 " + (i + 1);
            if (level.getBestSteps() != null) {
                buttonText += " (" + level.getBestSteps() + "s)";
            }
            levelButtons[i].setText(buttonText);
        }
    }

    public void showWinning(int level) {
        updateLevelButtons();
        SwingUtilities.invokeLater(() -> new LevelCompleteDialog(this, false, titleFont, otherFont).showLevelCompleteInfo(level));
    }
}