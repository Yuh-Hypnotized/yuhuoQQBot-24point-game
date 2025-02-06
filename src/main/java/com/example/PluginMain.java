package com.example;

import com.google.gson.Gson;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import net.mamoe.mirai.event.Event;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.Listener;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageRecallEvent.GroupRecall;
import net.mamoe.mirai.message.data.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.util.Stack;

public final class PluginMain extends JavaPlugin {
    public static final PluginMain INSTANCE = new PluginMain();

    private PluginMain() {
        super(new JvmPluginDescriptionBuilder("com.example.point24", "0.1.0")
                .name("24point")
                .author("Yuh_Hypnotized")

                .build());
    }

    @Override
    public void onEnable() {
        getLogger().info("Plugin loaded!");

        Config config = loadConfig();
        if (config == null) {
            getLogger().error("Failed to load config file!");
            return;
        }

        List<Long> whitelistedGroupID = config.whitelistedGroupID;
        List<Long> _24pointStatus_inGame = config._24pointStatus_inGame;
        List<user_24point> _24point_leaderBoard = config._24point_leaderBoard;

        Listener<GroupMessageEvent> listener = GlobalEventChannel.INSTANCE.subscribeAlways(GroupMessageEvent.class,
                event -> {
            MessageChain message = event.getMessage();
            long userID = event.getSender().getId();
            long groupID = event.getGroup().getId();

            if (whitelistedGroupID.contains(groupID)) {
                String messageString = message.contentToString().trim();
                if (messageString.startsWith("/24 start") && !_24pointStatus_inGame.contains(userID)) {

                    int num1 = (int)(Math.random() * 11) + 1;
                    int num2 = (int)(Math.random() * 11) + 1;
                    int num3 = (int)(Math.random() * 11) + 1;
                    int num4 = (int)(Math.random() * 11) + 1;
                    MessageChain chain = new MessageChainBuilder().append(new At(userID))
                            .append(" 你抽到了 [" + num1 + "] [" + num2 +
                            "] [" + num3 + "] [" + num4 +"]\n" + "请用以上四个数用+ - * / ()组合成结果为24的算式\n" +
                            "回答方法示例：/24 answer (13-9)*(11-5)\n" +
                                    "(注：用英文的括号，表达式中不要空格)").build();
                    event.getGroup().sendMessage(chain);
                    _24pointStatus_inGame.add(userID);
                }
                else if (messageString.startsWith("/24 start") && _24pointStatus_inGame.contains(userID)) {
                    MessageChain chain = new MessageChainBuilder().append(new At(userID))
                                    .append(" 你已经在游戏中！").build();
                    event.getGroup().sendMessage(chain);
                }
                else if (messageString.startsWith("/24 answer ") && _24pointStatus_inGame.contains(userID)) {
                    String[] commandParts = messageString.split("\\s+");
                    String expression = commandParts[2];

                    Stack<Double> values = new Stack<>();
                    Stack<Character> ops = new Stack<>();
                    for (int i=0; i<expression.length(); i++) {
                        char c = expression.charAt(i);
                        // 如果遇到数字，则解析整个数字（虽然数字都是整数，但用 double 处理）
                        if (Character.isDigit(c)) {
                            double num = 0;
                            while (i < expression.length() && Character.isDigit(expression.charAt(i))) {
                                num = num * 10 + (expression.charAt(i) - '0');
                                i++;
                            }
                            i--; // 补偿循环中多加的 i
                            values.push(num);
                        }
                        // 左括号直接入运算符栈
                        else if (c == '(') {
                            ops.push(c);
                        }
                        // 右括号，则计算直到遇到左括号
                        else if (c == ')') {
                            while (!ops.isEmpty() && ops.peek() != '(') {
                                char op = ops.pop();
                                double b = values.pop();
                                double a = values.pop();
                                double res = 0;
                                if (op == '+') res = a + b;
                                else if (op == '-') res = a - b;
                                else if (op == '*') res = a * b;
                                else if (op == '/') res = a / b;
                                values.push(res);
                            }
                            // 弹出左括号
                            if (!ops.isEmpty() && ops.peek() == '(') {
                                ops.pop();
                            }
                        }
                        // 运算符：+ - * /
                        else if (c == '+' || c == '-' || c == '*' || c == '/') {
                            // 当前运算符优先级：+和-为1，*和/为2
                            int currPrec = (c == '+' || c == '-') ? 1 : 2;
                            // 当运算符栈顶运算符优先级大于或等于当前运算符时，先计算栈内运算
                            while (!ops.isEmpty() && ops.peek() != '(') {
                                char top = ops.peek();
                                int topPrec = (top == '+' || top == '-') ? 1 : (top == '*' || top == '/') ? 2 : 0;
                                if (topPrec >= currPrec) {
                                    char op = ops.pop();
                                    double b = values.pop();
                                    double a = values.pop();
                                    double res = 0;
                                    if (op == '+') res = a + b;
                                    else if (op == '-') res = a - b;
                                    else if (op == '*') res = a * b;
                                    else if (op == '/') res = a / b;
                                    values.push(res);
                                } else {
                                    break;
                                }
                            }
                            // 当前运算符入栈
                            ops.push(c);
                        }
                    }
                    while (!ops.isEmpty()) {
                        char op = ops.pop();
                        double b = values.pop();
                        double a = values.pop();
                        double res = 0;
                        if (op == '+') res = a + b;
                        else if (op == '-') res = a - b;
                        else if (op == '*') res = a * b;
                        else if (op == '/') res = a / b;
                        values.push(res);
                    }

                    if (values.pop() == 24) {
                        boolean found = false;
                        int points = 1;
                        for (int i=0; i<config._24point_leaderBoard.size(); i++) {
                            if (config._24point_leaderBoard.get(i).userID == userID) {
                                points = ++config._24point_leaderBoard.get(i).userPoints;
                                updateConfig(config);
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            _24point_leaderBoard.add(new user_24point(userID, points));
                        }
                        MessageChain chain = new MessageChainBuilder().append(new At(userID))
                                        .append(" 回答正确，积分+1！\n" + "当前积分：" + points +
                                                ". 输入/24 start再来一把！").build();
                        event.getGroup().sendMessage(chain);
                        _24pointStatus_inGame.remove(userID);
                    }
                    else {
                        MessageChain chain = new MessageChainBuilder().append(new At(userID))
                                        .append(" 回答错误！").build();
                        event.getGroup().sendMessage(chain);
                    }
                }
                else if (messageString.startsWith("/24 answer ") && !_24pointStatus_inGame.contains(userID)) {
                    MessageChain chain = new MessageChainBuilder().append(new At(userID))
                                    .append(" 你还不在游戏中！").build();
                    event.getGroup().sendMessage(chain);
                }
                else if (messageString.startsWith("/24 lb")) {
                    List<user_24point> lb = config._24point_leaderBoard;
                    Collections.sort(lb, (a, b) -> Integer.compare(b.userPoints, a.userPoints));
                    MessageChainBuilder builder = new MessageChainBuilder();
                    builder.append("本群24点积分排行榜：\n").build();
                    int rank = 1;
                    for (int i=0; i<lb.size(); i++) {
                        if (event.getGroup().get(lb.get(i).userID) != null) {
                            builder.append(new PlainText((rank++) + ". "))
                                    .append(event.getGroup().get(lb.get(i).userID).getNick())
                                    .append(new PlainText("(" + lb.get(i).userID + ") - "
                                            + lb.get(i).userPoints + "分\n"));
                        }
                    }
                    MessageChain chain = builder.build();
                    event.getGroup().sendMessage(chain);
                }
                else if (messageString.startsWith("/24 ff") && _24pointStatus_inGame.contains(userID)) {
                    _24pointStatus_inGame.remove(userID);
                    MessageChain chain = new MessageChainBuilder().append(new At(userID))
                            .append(" 你放弃了这一题！\n输入/24 start再来一把！").build();
                    event.getGroup().sendMessage(chain);
                }
                else if (messageString.startsWith("/24 ff") && !_24pointStatus_inGame.contains(userID)) {
                    MessageChain chain = new MessageChainBuilder().append(new At(userID))
                            .append(" 你还不在游戏中！").build();
                    event.getGroup().sendMessage(chain);
                }
            }
                });
    }

    private Config loadConfig() {
        ObjectMapper objectMapper = new ObjectMapper();
        Config config;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config24.json")) {
            if (inputStream == null) {
                getLogger().error("Config file does not exist!");
                return null;
            }
            config = objectMapper.readValue(inputStream, Config.class);
            return config;
        }
        catch (Exception e) {
            getLogger().error("Failed to load config file!", e);
            return null;
        }
    }
    private void updateConfig(Config newConfig) {
        ObjectMapper objectMapper = new ObjectMapper();
        try{
            File file = new File("config24.json");
            objectMapper.writeValue(file, newConfig);
        }
        catch (Exception e) {
            getLogger().error("Failed to save config file!", e);
        }
    }
}