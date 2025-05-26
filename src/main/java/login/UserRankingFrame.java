package login;

import com.fasterxml.jackson.databind.ObjectMapper;
import game.TimeCounter;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class UserRankingFrame extends JDialog {
    private JTabbedPane tabbedPane;

    // Background related variables
    private BufferedImage backgroundImage;
    private int bgOffsetX = 0;
    private int bgOffsetY = 0;
    private double currentOffsetX = 0;
    private double currentOffsetY = 0;
    private final double dampingFactor = 0.2;
    private final double offsetFactorX = 0.1;
    private final double offsetFactorY = 0.1;
    private double targetOffsetX;
    private double targetOffsetY;
    private javax.swing.Timer smoothTransitionTimer;
    private int scaledWidth;
    private int scaledHeight;
    private int drawX;
    private int drawY;

    private Font customFont;

    public UserRankingFrame(Frame owner, boolean modal) {
        super(owner, "用户排名榜", modal);
        setSize(800, 600);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(owner);

        loadFont("/login/content_font.ttf", 24, Font.BOLD, false);

        loadBackgroundImage();
        calculateScaledSize();

        System.out.println("背景图片尺寸: " + (backgroundImage != null ? backgroundImage.getWidth() + "x" + backgroundImage.getHeight() : "未加载"));

        // Use custom background panel
        setContentPane(new BackgroundPanel());
        setLayout(new BorderLayout());

        // Create tabbed pane with transparent styling
        tabbedPane = new JTabbedPane();
        tabbedPane.setOpaque(false);
        tabbedPane.setBackground(new Color(0, 0, 0, 0));
        tabbedPane.setForeground(Color.WHITE);
        if (customFont != null) {
            tabbedPane.setFont(customFont);
        }
        tabbedPane.setUI(new BasicTabbedPaneUI() {
            @Override
            protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
                if (isSelected) {
                    g.setColor(new Color(255, 255, 255, 50));
                    g.fillRect(x, y, w, h);
                }
            }

            @Override
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
                // Don't paint content border
            }
        });
        add(tabbedPane, BorderLayout.CENTER);

        // Add back button
        JButton backButton = createMinecraftButton("返回", 650, 520, 120, 40);
        if (customFont != null) {
            backButton.setFont(customFont);
        }
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(null);
        buttonPanel.add(backButton);
        add(buttonPanel, BorderLayout.SOUTH);

        backButton.addActionListener(e -> dispose());

        try {
            displayRankings();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "加载排行榜数据失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void loadFont(String fontPath, float size, int style, boolean isTitle) {
        try {
            Font baseFont = Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream(fontPath));
            if (isTitle) {
                customFont = baseFont.deriveFont(style, size);
            } else {
                customFont = baseFont.deriveFont(style, size);
            }
        } catch (FontFormatException | IOException e) {
            System.err.println("字体加载失败: " + e.getMessage());
            if (isTitle) {
                customFont = new Font("楷体", style, (int) size);
            } else {
                customFont = new Font("微软雅黑", style, (int) size);
            }
        }
    }

    private void calculateScaledSize() {
        if (backgroundImage == null) return;

        int frameWidth = getWidth();
        int frameHeight = getHeight();
        int imageWidth = backgroundImage.getWidth();
        int imageHeight = backgroundImage.getHeight();

        double imageRatio = (double) imageWidth / imageHeight;
        double frameRatio = (double) frameWidth / frameHeight;

        if (frameRatio > imageRatio) {
            scaledWidth = frameWidth;
            scaledHeight = (int) (frameWidth / imageRatio);
        } else {
            scaledHeight = frameHeight;
            scaledWidth = (int) (frameHeight * imageRatio);
        }

        drawX = (frameWidth - scaledWidth) / 2;
        drawY = (frameHeight - scaledHeight) / 2;

        System.out.println("背景图片缩放后尺寸: " + scaledWidth + "x" + scaledHeight);
        System.out.println("背景图片绘制位置: (" + drawX + ", " + drawY + ")");
    }

    private void loadBackgroundImage() {
        try {
            URL imageUrl = getClass().getResource("/login/background2.jpg");
            if (imageUrl == null) {
                System.err.println("无法在类路径中找到2.jpg文件");
                File imageFile = new File("src/main/resources/background2.jpg");
                if (imageFile.exists()) {
                    backgroundImage = ImageIO.read(imageFile);
                    System.out.println("从文件系统加载背景图片成功");
                } else {
                    System.err.println("文件系统中也不存在2.jpg文件");
                    backgroundImage = null;
                }
            } else {
                backgroundImage = ImageIO.read(imageUrl);
                System.out.println("从类路径加载背景图片成功");
            }
        } catch (IOException e) {
            System.err.println("读取背景图片时发生IO异常: " + e.getMessage());
            backgroundImage = null;
        } catch (Exception e) {
            System.err.println("加载背景图片时发生未知异常: " + e.getMessage());
            backgroundImage = null;
        }
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
                if (customFont != null) {
                    g.setFont(customFont.deriveFont(16f));
                } else {
                    g.setFont(getFont().deriveFont(Font.BOLD, 16f));
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

    private void displayRankings() throws IOException {
        for (int l = 1; l <= 4; l++) {
            String levelName = "关卡" + l;

            java.util.List<Map<String, Object>> rankedUsers = new ArrayList<>();
            Map<String, Object> rankedUser;
            for (User user : LoginFrame.userArchive.getUsers()) {
                if(l>= user.getProgress())
                    continue;
                rankedUser = new HashMap<>();
                rankedUser.put("user", user.getName());
                rankedUser.put("time", user.getLevels().get(l-1).getBestTime());
                rankedUser.put("steps", user.getLevels().get(l-1).getBestSteps());
                rankedUsers.add(rankedUser);
            }

            rankedUsers.sort(Comparator.comparingLong(u -> (Long) u.get("time")));

            JPanel rankingPanel = new JPanel();
            rankingPanel.setOpaque(false);
            rankingPanel.setLayout(new BorderLayout());

            JLabel titleLabel = new JLabel(levelName + " 排行榜");
            if (customFont != null) {
                titleLabel.setFont(customFont.deriveFont(24f));
            } else {
                titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 24));
            }
            titleLabel.setForeground(new Color(255, 255, 255, 220));
            titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
            rankingPanel.add(titleLabel, BorderLayout.NORTH);

            JTextArea rankingText = new JTextArea();
            rankingText.setEditable(false);
            rankingText.setOpaque(false);
            rankingText.setBackground(new Color(0, 0, 0, 0));
            rankingText.setForeground(new Color(255, 255, 255, 220));
            if (customFont != null) {
                rankingText.setFont(customFont.deriveFont(18f));
            } else {
                rankingText.setFont(new Font("微软雅黑", Font.PLAIN, 18));
            }
            rankingText.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(rankedUsers.size(), 10); i++) {
                rankedUser = rankedUsers.get(i);
                String username = (String) rankedUser.get("user");
                Long time = (Long) rankedUser.get("time");
                Integer steps = (Integer) rankedUser.get("steps");

                sb.append(String.format("No.%d\t %s\t 步数: %d\t 时间: %s\n", i + 1, username, steps, TimeCounter.formatTime(time)));
            }

            if (rankedUsers.isEmpty()) {
                sb.append("暂无数据\n");
            }

            rankingText.setText(sb.toString());
            JScrollPane scrollPane = new JScrollPane(rankingText);
            scrollPane.setOpaque(false);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            scrollPane.getViewport().setOpaque(false);

            // Custom scrollbar styling
            JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
            verticalScrollBar.setOpaque(false);
            verticalScrollBar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
                @Override
                protected void configureScrollBarColors() {
                    this.thumbColor = new Color(255, 255, 255, 100);
                }

                @Override
                protected JButton createDecreaseButton(int orientation) {
                    return createInvisibleButton();
                }

                @Override
                protected JButton createIncreaseButton(int orientation) {
                    return createInvisibleButton();
                }

                private JButton createInvisibleButton() {
                    JButton button = new JButton();
                    button.setOpaque(false);
                    button.setBorder(BorderFactory.createEmptyBorder());
                    button.setContentAreaFilled(false);
                    return button;
                }

                @Override
                protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                    g.setColor(new Color(0, 0, 0, 30));
                    g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
                }

                @Override
                protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(thumbColor);
                    g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2, thumbBounds.width - 4, thumbBounds.height - 4, 5, 5);
                }
            });

            rankingPanel.add(scrollPane, BorderLayout.CENTER);
            tabbedPane.addTab(levelName, rankingPanel);
        }
    }

    private class BackgroundPanel extends JPanel {
        public BackgroundPanel() {
            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int centerX = getWidth() / 2;
                    int centerY = getHeight() / 2;
                    int dx = e.getX() - centerX;
                    int dy = e.getY() - centerY;
                    targetOffsetX = -dx * offsetFactorX;
                    targetOffsetY = -dy * offsetFactorY;

                    if (smoothTransitionTimer != null && smoothTransitionTimer.isRunning()) {
                        return;
                    }

                    smoothTransitionTimer = new javax.swing.Timer(20, ae -> {
                        currentOffsetX += (targetOffsetX - currentOffsetX) * dampingFactor;
                        currentOffsetY += (targetOffsetY - currentOffsetY) * dampingFactor;

                        int maxOffsetX = (scaledWidth - getWidth()) / 2;
                        bgOffsetX = (int) Math.max(-maxOffsetX, Math.min(maxOffsetX, currentOffsetX));
                        int maxOffsetY = (scaledHeight - getHeight()) / 2;
                        bgOffsetY = (int) Math.max(-maxOffsetY, Math.min(maxOffsetY, currentOffsetY));

                        repaint();

                        if (Math.abs(currentOffsetX - targetOffsetX) < 1 && Math.abs(currentOffsetY - targetOffsetY) < 1) {
                            smoothTransitionTimer.stop();
                        }
                    });
                    smoothTransitionTimer.start();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

            if (backgroundImage != null) {
                // Draw background image with parallax effect
                g2d.drawImage(backgroundImage, drawX + bgOffsetX, drawY + bgOffsetY, scaledWidth, scaledHeight, this);

                // Add semi-transparent overlay for better text readability
                g2d.setColor(new Color(0, 0, 0, 80));
                g2d.fillRect(0, 0, getWidth(), getHeight());
            } else {
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        }
    }
}