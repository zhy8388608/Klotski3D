package login;

import game.TimeCounter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LevelCompleteDialog extends JDialog {
    private JLabel titleLabel; // 新增标题标签
    private JLabel levelCompleteLabel;
    private JLabel stepsLabel;
    private JLabel timeLabel;
    private JButton nextLevelBtn;
    private JButton restartLevelBtn;
    private JButton backToSelectionBtn;
    private LoginFrame parentFrame;
    private int currentLevelIndex;
    private Font titleFont;
    private Font otherFont;
    private int bgOffsetX = 0; // 背景水平偏移量
    private int bgOffsetY = 0; // 背景垂直偏移量
    private double currentOffsetX = 0;
    private double currentOffsetY = 0;
    private final double dampingFactor = 0.2; // 阻尼系数，可根据需要调整
    private final double offsetFactorX = 0.1; // 减小左右方向的偏移系数
    private final double offsetFactorY = 0.1; // 减小上下方向的偏移系数
    private double targetOffsetX;
    private double targetOffsetY;
    private Timer smoothTransitionTimer;
    private int layoutOffsetX; // 布局水平偏移量
    private int layoutOffsetY; // 布局垂直偏移量
    private Timer shineTimer; // 闪耀动效定时器
    private Color[] shineColors = {
            new Color(255, 215, 0), // 金色
            new Color(255, 0, 0),   // 红色
            new Color(255, 165, 0), // 橙色
            new Color(255, 255, 0), // 黄色
            new Color(0, 128, 0),   // 绿色
            new Color(0, 0, 255),   // 蓝色
            new Color(75, 0, 130),  // 靛色
            new Color(238, 130, 238), // 紫色
            new Color(255, 255, 255)};  // 白色
    private int colorIndex = 0; // 当前颜色索引
    private int colorStep = 0; // 颜色渐变步骤
    private final int totalSteps = 10; // 颜色渐变总步骤
    private List<Firework> fireworks;
    private Timer fireworkTimer;
    // 全局字体变量
    private Font customFont;
    private Font defaultFont = new Font("Arial", Font.PLAIN, 12);

    // 新增：选关按钮列表
    private List<JButton> levelSelectionButtons;

    private JButton rankingBtn;

    public LevelCompleteDialog(LoginFrame parent, boolean modal, Font titleFont, Font otherFont) {
        super(parent, modal); // 确保模态属性设置为true
        this.parentFrame = parent;
        this.titleFont = titleFont;
        this.otherFont = otherFont;
        fireworks = new ArrayList<>();
        levelSelectionButtons = new ArrayList<>();
        loadCustomFont(); // 先加载字体
        initComponents();
        setupLayout();
        setupListeners();
        setSize(800, 500);
        setLocationRelativeTo(parent);
        // 修改关闭操作，点击关闭按钮时关闭对话框
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(false);
        startShineEffect(); // 启动闪耀动效
        startFireworkEffect(); // 启动烟花效果
        // 初始时隐藏选关按钮
        setLevelSelectionButtonsVisible(false);
    }

    private void loadCustomFont() {
        try {
            System.out.println("尝试加载自定义字体: content_font.ttf");

            // 从类路径根目录加载字体文件（适用于Maven/Gradle项目结构）
            InputStream fontStream = getClass().getClassLoader().getResourceAsStream("login/content_font.ttf");

            if (fontStream != null) {
                System.out.println("找到字体文件，开始加载...");

                // 加载字体
                customFont = Font.createFont(Font.TRUETYPE_FONT, fontStream);

                // 注册字体到图形环境
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                boolean registered = ge.registerFont(customFont);

                if (registered) {
                    System.out.println("字体注册成功！");
                    System.out.println("字体名称: " + customFont.getFontName());
                    System.out.println("字体家族: " + customFont.getFamily());

                    // 派生一个默认大小的字体供后续使用
                    customFont = customFont.deriveFont(20f);
                } else {
                    System.err.println("字体注册失败！");
                    customFont = null;
                }

                fontStream.close();
            } else {
                System.err.println("无法找到字体文件: content_font.ttf");
                System.err.println("请确保字体文件位于src/main/resources目录下");
                customFont = null;
            }
        } catch (IOException e) {
            System.err.println("读取字体文件时出错: " + e.getMessage());
            customFont = null;
        } catch (FontFormatException e) {
            System.err.println("字体格式错误: " + e.getMessage());
            customFont = null;
        } catch (Exception e) {
            System.err.println("加载字体时发生未知错误: " + e.getMessage());
            customFont = null;
        } finally {
            // 如果字体加载失败，使用默认字体

            if (customFont == null) {
                System.out.println("使用默认字体: " + defaultFont.getFamily());
                customFont = defaultFont;
            }
        }
    }

    private void initComponents() {
        // 标题标签（从登录界面移植）
        titleLabel = new JLabel("CONGRATULATIONS") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                super.paintComponent(g2d);
            }
        };
        if (titleFont != null) {
            // 将字体大小从30f略微调大到36f
            titleLabel.setFont(titleFont.deriveFont(34f));
        } else {
            titleLabel.setFont(new Font("楷体", Font.BOLD, 36));
        }
        titleLabel.setForeground(new Color(255, 215, 0));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setVerticalAlignment(SwingConstants.TOP);

        // 通关标签
        levelCompleteLabel = new JLabel("关卡完成!");
        if (titleFont != null) {
            levelCompleteLabel.setFont(titleFont.deriveFont(50f));
        } else {
            levelCompleteLabel.setFont(new Font("楷体", Font.BOLD, 50));
        }
        levelCompleteLabel.setForeground(Color.WHITE);
        levelCompleteLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // 步数标签
        stepsLabel = new JLabel("步数: --");
        if (otherFont != null) {
            stepsLabel.setFont(otherFont.deriveFont(24f));
        } else {
            stepsLabel.setFont(new Font("微软雅黑", Font.PLAIN, 24));
        }
        stepsLabel.setForeground(Color.WHITE);
        stepsLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // 时间标签
        timeLabel = new JLabel("时间: --");
        if (otherFont != null) {
            timeLabel.setFont(otherFont.deriveFont(24f));
        } else {
            timeLabel.setFont(new Font("微软雅黑", Font.PLAIN, 24));
        }
        timeLabel.setForeground(Color.WHITE);
        timeLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // 下一关按钮
        nextLevelBtn = createMinecraftButton("下一关", 300, 320, 200, 50);

        // 重新开始按钮
        restartLevelBtn = createMinecraftButton("重新开始", 300, 380, 200, 50);

        // 返回选关界面按钮
        backToSelectionBtn = createMinecraftButton("返回选关", 300, 440, 200, 50);

        int buttonWidth = 200;  // 与返回选关按钮宽度一致
        int buttonHeight = 50;
        int buttonSpacing = 15; // 按钮之间的间距
        int startX = 500 + layoutOffsetX; // 与返回选关按钮X坐标一致
        int startY = 150 + layoutOffsetY; // 起始Y坐标

        for (int i = 1; i <= 4; i++) {
            JButton levelButton = createMinecraftButton("关卡 " + i,
                    startX,
                    startY + (i - 1) * (buttonHeight + buttonSpacing),
                    buttonWidth,
                    buttonHeight);
            levelButton.addActionListener(e -> {
                int levelIndex = Integer.parseInt(levelButton.getText().split(" ")[1]);
                parentFrame.startLevel(levelIndex);
                dispose();
            });
            levelSelectionButtons.add(levelButton);
        }
        rankingBtn = createMinecraftButton("用户排名", 500, 402, 200, 50);
    }

    private void setupLayout() {
        // 创建带背景的内容面板
        JPanel contentPane = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // 将 Graphics 对象转换为 Graphics2D 对象
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // 从父窗口获取背景图
                if (parentFrame.getBackgroundImage() != null) {
                    g2d.drawImage(parentFrame.getBackgroundImage(),
                            parentFrame.getDrawX() + bgOffsetX,
                            parentFrame.getDrawY() + bgOffsetY,
                            parentFrame.getScaledWidth(),
                            parentFrame.getScaledHeight(),
                            this);
                }
                // 绘制烟花
                for (Firework firework : fireworks) {
                    firework.draw(g2d);
                }
            }
        };
        contentPane.setLayout(null);
        setContentPane(contentPane);

        // 定义布局偏移量（向右和向上）
        layoutOffsetX = 200;
        layoutOffsetY = -50; // 负值表示向上移动

        // 标题标签（从登录界面移植）
        titleLabel.setBounds(200, 150 + layoutOffsetY, 800, 100);
        contentPane.add(titleLabel);

        // 通关标签
        levelCompleteLabel.setBounds(200, 100 + layoutOffsetY, 800 - layoutOffsetX, 60);
        contentPane.add(levelCompleteLabel);

        // 步数标签
        stepsLabel.setBounds(300, 200 + layoutOffsetY, 800 - layoutOffsetX, 30);
        contentPane.add(stepsLabel);

        // 时间标签
        timeLabel.setBounds(300, 233 + layoutOffsetY, 800 - layoutOffsetX, 30);
        contentPane.add(timeLabel);

        // 下一关按钮
        nextLevelBtn.setBounds(300 + layoutOffsetX, 270 + layoutOffsetY, 200, 50);
        contentPane.add(nextLevelBtn);

        // 重新开始按钮
        restartLevelBtn.setBounds(300 + layoutOffsetX, 330 + layoutOffsetY, 200, 50);
        contentPane.add(restartLevelBtn);

        // 返回选关界面按钮
        backToSelectionBtn.setBounds(300 + layoutOffsetX, 390 + layoutOffsetY, 200, 50);
        contentPane.add(backToSelectionBtn);

        // 新增：添加选关按钮到面板
        for (JButton button : levelSelectionButtons) {
            contentPane.add(button);
        }

        // 添加用户排行按钮到面板
        contentPane.add(rankingBtn);

        // 监听鼠标移动事件
        MouseAdapter mouseMoveAdapter = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int centerX = getWidth() / 2;
                int centerY = getHeight() / 2;
                int dx = e.getX() - centerX;
                int dy = e.getY() - centerY;
                // 计算目标偏移量
                targetOffsetX = -dx * offsetFactorX;
                targetOffsetY = -dy * offsetFactorY;

                if (smoothTransitionTimer != null && smoothTransitionTimer.isRunning()) {
                    return;
                }

                // 开始平滑过渡
                smoothTransitionTimer = new Timer(20, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        // 根据阻尼系数更新当前偏移量
                        currentOffsetX += (targetOffsetX - currentOffsetX) * dampingFactor;
                        currentOffsetY += (targetOffsetY - currentOffsetY) * dampingFactor;

                        // 确保背景图不会露出边框
                        int maxOffsetX = (parentFrame.getScaledWidth() - getWidth()) / 2;
                        bgOffsetX = (int) Math.max(-maxOffsetX, Math.min(maxOffsetX, currentOffsetX));
                        int maxOffsetY = (parentFrame.getScaledHeight() - getHeight()) / 2;
                        bgOffsetY = (int) Math.max(-maxOffsetY, Math.min(maxOffsetY, currentOffsetY));

                        contentPane.repaint();

                        // 判断是否接近目标偏移量
                        if (Math.abs(currentOffsetX - targetOffsetX) < 1 && Math.abs(currentOffsetY - targetOffsetY) < 1) {
                            smoothTransitionTimer.stop();
                        }
                    }
                });
                smoothTransitionTimer.start();
            }
        };

        contentPane.addMouseMotionListener(mouseMoveAdapter);
    }

    private void setupListeners() {
        // 下一关按钮事件
        nextLevelBtn.addActionListener(e -> {
            int nextLevel = currentLevelIndex + 1;
            if (nextLevel <= 4) {
                parentFrame.startLevel(nextLevel);
                dispose();
            } else {
                // 所有关卡已完成，可以显示全部通关的消息
                JOptionPane.showMessageDialog(this, "恭喜你完成了所有关卡!", "通关成功", JOptionPane.INFORMATION_MESSAGE);
                //parentFrame.showLevelSelection();
                dispose();
            }
        });

        // 重新开始按钮事件
        restartLevelBtn.addActionListener(e -> {
            parentFrame.startLevel(currentLevelIndex);
            dispose();
        });

        // 返回选关界面按钮事件
        backToSelectionBtn.addActionListener(e -> {
            dispose();
        });

// 用户排行按钮事件
        rankingBtn.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                // 创建非模态对话框
                UserRankingFrame rankingFrame = new UserRankingFrame(
                        (Frame) SwingUtilities.getWindowAncestor(LevelCompleteDialog.this),
                        false // 设置为非模态
                );
                rankingFrame.setVisible(true);
            });
        });
    }

    private JButton createMinecraftButton(String text, int x, int y, int width, int height) {
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

                // 使用自定义字体，大小为20
                if (customFont != null) {
                    Font buttonFont = customFont.deriveFont(Font.PLAIN, 20f);
                    g.setFont(buttonFont);
                } else {
                    // 回退到默认字体
                    Font buttonFont = g.getFont().deriveFont(20f);
                    g.setFont(buttonFont);
                }

                FontMetrics fm = g.getFontMetrics();
                int xPos = (getWidth() - fm.stringWidth(getText())) / 2;
                int yPos = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                g.drawString(getText(), xPos, yPos);
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
        button.setBounds(x, y, width, height);
        button.setRolloverEnabled(true);

        return button;
    }

    // 更新并显示通关信息
    public void showLevelCompleteInfo(int levelIndex) {
        this.currentLevelIndex = levelIndex;
        // 假设的 Level 类
        Level level = parentFrame.getCurrentUser().getLevels().get(levelIndex - 1);

        // 更新步数和时间显示
        stepsLabel.setText("步数: " + (level.getStepDirections().length()/2 + 1));
        timeLabel.setText("时间: " + TimeCounter.formatTime(level.getHistoryTime()));

        // 显示对话框
        setVisible(true);
    }

    // 启动闪耀动效
    private void startShineEffect() {
        shineTimer = new Timer(100, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (colorStep < totalSteps) {
                    Color startColor = shineColors[colorIndex];
                    Color endColor = shineColors[(colorIndex + 1) % shineColors.length];
                    float ratio = (float) colorStep / totalSteps;
                    int red = (int) (startColor.getRed() + ratio * (endColor.getRed() - startColor.getRed()));
                    int green = (int) (startColor.getGreen() + ratio * (endColor.getGreen() - startColor.getGreen()));
                    int blue = (int) (startColor.getBlue() + ratio * (endColor.getBlue() - startColor.getBlue()));
                    titleLabel.setForeground(new Color(red, green, blue));
                    colorStep++;
                } else {
                    colorIndex = (colorIndex + 1) % shineColors.length;
                    colorStep = 0;
                }
            }
        });
        shineTimer.start();
    }

    // 定义对象池
    private List<Firework> fireworkPool = new ArrayList<>();
    private List<Particle> particlePool = new ArrayList<>();

    // 启动烟花效果
    private void startFireworkEffect() {
        fireworkTimer = new Timer(30, new ActionListener() {
            Random random = new Random();

            @Override
            public void actionPerformed(ActionEvent e) {
                if (random.nextInt(100) < 5) {
                    // 计算标题周围的随机位置
                    Rectangle titleBounds = titleLabel.getBounds();
                    int padding = 50; // 文字周围的填充区域

                    // 在标题周围随机选择位置
                    int spawnX, spawnY;
                    if (random.nextBoolean()) {
                        // 在左右两侧
                        spawnX = random.nextBoolean() ?
                                titleBounds.x - padding :
                                titleBounds.x + titleBounds.width + padding;
                        spawnY = titleBounds.y + random.nextInt(titleBounds.height);
                    } else {
                        // 在上下两侧
                        spawnX = titleBounds.x + random.nextInt(titleBounds.width);
                        spawnY = random.nextBoolean() ?
                                titleBounds.y - padding :
                                titleBounds.y + titleBounds.height + padding;
                    }

                    // 生成朝向文字的烟花，加快烟花的发射速度，将速度范围从 2-5 提高到 3-6
                    int textCenterX = titleBounds.x + titleBounds.width / 2;
                    int textCenterY = titleBounds.y + titleBounds.height / 2;
                    double angle = Math.atan2(textCenterY - spawnY, textCenterX - spawnX);
                    double speed = random.nextDouble() * 3 + 4;
                    double vx = Math.cos(angle) * speed;
                    double vy = Math.sin(angle) * speed;

                    // 从对象池获取或创建烟花
                    Firework firework;
                    if (fireworkPool.isEmpty()) {
                        firework = new Firework(spawnX, spawnY, vx, vy);
                    } else {
                        firework = fireworkPool.remove(fireworkPool.size() - 1);
                        firework.reset(spawnX, spawnY, vx, vy);
                    }
                    fireworks.add(firework);
                }

                // 更新并移除过期的烟花
                for (int i = fireworks.size() - 1; i >= 0; i--) {
                    Firework firework = fireworks.get(i);
                    firework.update();
                    if (firework.isExpired()) {
                        fireworks.remove(i);
                        fireworkPool.add(firework); // 归还到对象池
                    }
                }
                repaint();
            }
        });
        fireworkTimer.start();
    }

    @Override
    public void dispose() {
        if (shineTimer != null && shineTimer.isRunning()) {
            shineTimer.stop();
        }
        if (fireworkTimer != null && fireworkTimer.isRunning()) {
            fireworkTimer.stop();
        }
        super.dispose();
    }

    // 烟花类
    private class Firework {
        private double x;
        private double y;
        private double vx;  // 烟花整体速度（飞向文字）
        private double vy;
        private List<Particle> particles;
        private int life;
        private boolean exploded; // 是否已爆炸

        // 新增构造函数，支持有初始速度的烟花
        public Firework(double x, double y, double vx, double vy) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            particles = new ArrayList<>();
            life = 60;
            exploded = false;
        }

        // 重置方法
        public void reset(double x, double y, double vx, double vy) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            particles.clear();
            life = 60;
            exploded = false;
        }

        public void update() {
            if (!exploded) {
                // 飞向文字
                x += vx;
                y += vy;

                // 检测是否到达文字区域附近
                Rectangle titleBounds = titleLabel.getBounds();
                int textCenterX = titleBounds.x + titleBounds.width / 2;
                int textCenterY = titleBounds.y + titleBounds.height / 2;
                double distance = Math.sqrt(Math.pow(x - textCenterX, 2) + Math.pow(y - textCenterY, 2));

                // 到达文字附近或生命周期过半时爆炸
                if (distance < 30 || life < 30) {
                    explode();
                }
            } else {
                // 爆炸后更新所有粒子
                for (int i = particles.size() - 1; i >= 0; i--) {
                    particles.get(i).update();
                    if (particles.get(i).isExpired()) {
                        particles.remove(i);
                    }
                }
            }
            life--;
        }

        // 触发爆炸效果
        private void explode() {
            exploded = true;
            Random random = new Random();
            Color color = new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));

            // 生成爆炸粒子
            for (int i = 0; i < 50; i++) {
                double angle = random.nextDouble() * 2 * Math.PI;
                double speed = random.nextDouble() * 5 + 1;
                double vx = Math.cos(angle) * speed;
                double vy = Math.sin(angle) * speed;
                particles.add(new Particle(x, y, vx, vy, color));
            }
        }

        public void draw(Graphics2D g2d) {
            if (!exploded) {
                // 绘制飞向文字的烟花
                g2d.setColor(Color.WHITE);
                g2d.fillOval((int) (x - 2), (int) (y - 2), 4, 4);
            } else {
                // 绘制爆炸后的粒子
                for (Particle particle : particles) {
                    particle.draw(g2d);
                }
            }
        }

        public boolean isExpired() {
            return life <= 0 && (exploded && particles.isEmpty());
        }
    }

    // 粒子类
    private class Particle {
        private double x;
        private double y;
        private double vx;  // 水平速度
        private double vy;  // 垂直速度
        private Color color;
        private int life;
        private float size; // 粒子大小
        private float brightness; // 亮度，影响透明度

        public Particle(double x, double y, double vx, double vy, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.life = 60; // 生命周期帧数
            this.size = (float) (Math.random() * 2 + 1);
            this.brightness = 1.0f; // 初始亮度
        }

        // 重置方法
        public void reset(double x, double y, double vx, double vy, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.life = 60;
            this.size = (float) (Math.random() * 2 + 1);
            this.brightness = 1.0f;
        }

        public void update() {
            x += vx;
            y += vy;
            vy += 0.3; // 重力
            vx *= 0.98; // 空气阻力
            vy *= 0.98; // 空气阻力

            // 粒子生命周期管理
            life--;
            if (life < 30) { // 生命周期后半段逐渐消失
                brightness = (float) life / 30;
            }
        }

        public void draw(Graphics2D g2d) {
            // 创建半透明颜色
            Color drawColor = new Color(
                    color.getRed(),
                    color.getGreen(),
                    color.getBlue(),
                    (int) (255 * brightness)
            );

            // 绘制发光效果
            g2d.setColor(new Color(drawColor.getRed(), drawColor.getGreen(), drawColor.getBlue(), Math.max(10, drawColor.getAlpha() / 3)));
            g2d.fillOval((int) (x - size * 2), (int) (y - size * 2), (int) (size * 4), (int) (size * 4));

            // 绘制粒子主体
            g2d.setColor(drawColor);
            g2d.fillOval((int) (x - size), (int) (y - size), (int) (size * 2), (int) (size * 2));
        }

        public boolean isExpired() {
            return life <= 0;
        }
    }

    // 新增：设置选关按钮的可见性
    private void setLevelSelectionButtonsVisible(boolean visible) {
        for (JButton button : levelSelectionButtons) {
            button.setVisible(visible);
        }
    }
}