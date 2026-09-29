package ckpb1.modules.automation;

import ckpb1.client.core.Category;
import ckpb1.client.core.ChatUtil;
import ckpb1.client.core.Module;
import ckpb1.client.core.StatusOverlay;
import ckpb1.client.core.setting.BoolSetting;
import ckpb1.client.core.setting.ListSetting;
import ckpb1.client.core.setting.ModeSetting;
import ckpb1.client.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;

/**
 * BedWars Automation (test environments): a configurable state machine that
 * chains the other CK_PB1 modules - collecting resources, running shop
 * commands, bridging, attacking targets and destroying beds - and reports
 * live match status on the HUD.
 *
 * <p>Every stage can be enabled/disabled and configured individually. The
 * automation only drives CK_PB1's own modules; it never talks to public
 * servers.</p>
 */
public final class BedWarsAutomation extends Module {

    public enum Stage {
        IDLE, COLLECT, BUY, BRIDGE, ATTACK, DESTROY, DONE
    }

    public final BoolSetting collectStage = add(new BoolSetting("Enable Collect Stage",
            "Gather the configured resource first", true));
    public final ModeSetting resource = add(new ModeSetting("Resource",
            "Resource to collect", "Iron", "Gold", "Diamond", "Emerald"));
    public final NumberSetting resourceAmount = add(new NumberSetting("Resource Amount",
            "How many items to collect before advancing", 24, 1, 128, 1));
    public final BoolSetting buyStage = add(new BoolSetting("Enable Buy Stage",
            "Run the configured shop commands (test servers with shop commands)", false));
    public final ListSetting buyCommands = add(new ListSetting("Buy Commands",
            "Commands executed during the buy stage (without leading /)", List.of("shop")));
    public final BoolSetting bridgeStage = add(new BoolSetting("Enable Bridge Stage",
            "Activate the Bridge Assistant toward the enemy side", true));
    public final NumberSetting bridgeSeconds = add(new NumberSetting("Bridge Seconds",
            "Seconds of bridging before advancing", 15, 5, 120, 5, "s"));
    public final BoolSetting attackStage = add(new BoolSetting("Enable Attack Stage",
            "Attack the configured players with Kill Farm", true));
    public final ListSetting attackTargets = add(new ListSetting("Attack Targets",
            "Players attacked during the attack stage", List.of()));
    public final BoolSetting destroyStage = add(new BoolSetting("Enable Destroy Stage",
            "Run Bed Destroyer V2 on the nearest enemy bed", true));
    public final NumberSetting stageDelay = add(new NumberSetting("Stage Delay",
            "Pause between stages in seconds", 1, 0, 10, 0.5, "s"));

    private Stage stage = Stage.IDLE;
    private long stageEnteredAt;
    private int buyCommandIndex;
    private long nextBuyCommandAt;
    private boolean pendingNext;
    private long pendingSince;

    public BedWarsAutomation() {
        super("BedWars Automation",
                "Chains collect -> buy -> bridge -> attack -> destroy stages automatically (test environments)",
                Category.AUTOMATION);
    }

    public static String[] statusLines() {
        BedWarsAutomation self = current();
        if (self == null || !self.isEnabled()) {
            return new String[0];
        }
        List<String> lines = new ArrayList<>();
        lines.add("stage: " + self.stage);
        long elapsed = (System.currentTimeMillis() - self.stageEnteredAt) / 1000;
        lines.add("stage time: " + elapsed + "s");
        switch (self.stage) {
            case COLLECT -> lines.add(self.resource.get() + ": "
                    + self.countResource(self.resource.get()) + "/" + self.resourceAmount.getInt());
            case BUY -> lines.add("command " + (self.buyCommandIndex + 1) + "/"
                    + Math.max(1, self.buyCommands.get().size()));
            case BRIDGE -> lines.add("bridging...");
            case ATTACK -> lines.add("targets: " + self.attackTargets.get().size());
            case DESTROY -> lines.add("destroying beds...");
            default -> {
            }
        }
        return lines.toArray(new String[0]);
    }

    private static BedWarsAutomation current() {
        var modules = ckpb1.client.CKPB1Client.modules();
        if (modules == null) {
            return null;
        }
        var m = modules.byName("BedWars Automation");
        return m instanceof BedWarsAutomation automation ? automation : null;
    }

    @Override
    protected void onEnable() {
        pendingNext = false;
        enterStage(Stage.IDLE);
    }

    @Override
    protected void onDisable() {
        disableSubModules();
        StatusOverlay.clear("Automation");
    }

    private void disableSubModules() {
        setModuleEnabled("Resource Assistant", false);
        setModuleEnabled("Bridge Assistant", false);
        setModuleEnabled("Kill Farm", false);
        setModuleEnabled("Bed Destroyer V2", false);
    }

    private void setModuleEnabled(String name, boolean enabled) {
        Module m = ckpb1.client.CKPB1Client.modules().byName(name);
        if (m != null && m.isEnabled() != enabled) {
            m.setEnabled(enabled, false);
        }
    }

    private void enterStage(Stage next) {
        stage = next;
        stageEnteredAt = System.currentTimeMillis();
        buyCommandIndex = 0;
        nextBuyCommandAt = 0;
        ChatUtil.message("§bAutomation §7-> §f" + next);
        switch (next) {
            case COLLECT -> {
                Module ra = ckpb1.client.CKPB1Client.modules().byName("Resource Assistant");
                if (ra instanceof ckpb1.modules.world.ResourceAssistant assistant) {
                    assistant.resources.clear();
                    assistant.resources.add(resourceBlockId());
                    assistant.autoWalk.setOn(true);
                    assistant.setEnabled(true, false);
                }
            }
            case BRIDGE -> setModuleEnabled("Bridge Assistant", true);
            case ATTACK -> {
                Module kf = ckpb1.client.CKPB1Client.modules().byName("Kill Farm");
                if (kf instanceof ckpb1.modules.combat.KillFarm killFarm) {
                    killFarm.targetList.set(attackTargets.get());
                    killFarm.setEnabled(!attackTargets.isEmpty(), false);
                }
            }
            case DESTROY -> setModuleEnabled("Bed Destroyer V2", true);
            case DONE -> {
                disableSubModules();
                StatusOverlay.set("Automation", List.of("§amatch routine finished"));
                setEnabled(false, false);
            }
            default -> {
            }
        }
    }

    private String resourceBlockId() {
        return switch (resource.get()) {
            case "Gold" -> "gold_ore";
            case "Diamond" -> "diamond_ore";
            case "Emerald" -> "emerald_ore";
            default -> "iron_ore";
        };
    }

    private Item[] resourceItems() {
        return switch (resource.get()) {
            case "Gold" -> new Item[]{Items.RAW_GOLD, Items.GOLD_ORE, Items.GOLD_INGOT};
            case "Diamond" -> new Item[]{Items.DIAMOND, Items.DIAMOND_ORE};
            case "Emerald" -> new Item[]{Items.EMERALD, Items.EMERALD_ORE};
            default -> new Item[]{Items.RAW_IRON, Items.IRON_ORE, Items.IRON_INGOT};
        };
    }

    private int countResource(String resourceName) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return 0;
        }
        int count = 0;
        for (Item wanted : resourceItems()) {
            for (var stack : mc.player.getInventory().main) {
                if (stack.getItem() == wanted) {
                    count += stack.getCount();
                }
            }
        }
        return count;
    }

    /** First enabled stage at or after the given stage. */
    private Stage nextEnabledStage(Stage from) {
        return switch (from) {
            case COLLECT -> collectStage.isOn() ? Stage.COLLECT : nextEnabledStage(Stage.BUY);
            case BUY -> buyStage.isOn() ? Stage.BUY : nextEnabledStage(Stage.BRIDGE);
            case BRIDGE -> bridgeStage.isOn() ? Stage.BRIDGE : nextEnabledStage(Stage.ATTACK);
            case ATTACK -> attackStage.isOn() ? Stage.ATTACK : nextEnabledStage(Stage.DESTROY);
            case DESTROY -> destroyStage.isOn() ? Stage.DESTROY : Stage.DONE;
            default -> Stage.DONE;
        };
    }

    /** Marks the current stage as complete; advances after the configured delay. */
    private void completeStage() {
        pendingNext = true;
        pendingSince = System.currentTimeMillis();
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            return;
        }
        long inStage = System.currentTimeMillis() - stageEnteredAt;
        long delayMs = (long) (stageDelay.get() * 1000);

        // stage transition after delay
        if (pendingNext) {
            if (System.currentTimeMillis() - pendingSince >= delayMs) {
                pendingNext = false;
                Stage next = switch (stage) {
                    case IDLE, COLLECT -> nextEnabledStage(Stage.BUY);
                    case BUY -> nextEnabledStage(Stage.BRIDGE);
                    case BRIDGE -> nextEnabledStage(Stage.ATTACK);
                    case ATTACK -> nextEnabledStage(Stage.DESTROY);
                    default -> Stage.DONE;
                };
                enterStage(next);
            }
        } else {
            switch (stage) {
                case IDLE -> {
                    if (inStage >= 500) {
                        enterStage(nextEnabledStage(Stage.COLLECT));
                    }
                }
                case COLLECT -> {
                    if (countResource(resource.get()) >= resourceAmount.getInt()) {
                        setModuleEnabled("Resource Assistant", false);
                        completeStage();
                    }
                }
                case BUY -> {
                    List<String> commands = buyCommands.get();
                    if (buyCommandIndex >= commands.size()) {
                        completeStage();
                    } else if (System.currentTimeMillis() >= nextBuyCommandAt) {
                        String command = commands.get(buyCommandIndex++);
                        ChatUtil.message("§7shop: §f" + command);
                        if (mc.player.networkHandler != null) {
                            mc.player.networkHandler.sendCommand(command);
                        }
                        nextBuyCommandAt = System.currentTimeMillis() + 1500;
                    }
                }
                case BRIDGE -> {
                    if (inStage >= (long) (bridgeSeconds.get() * 1000)) {
                        setModuleEnabled("Bridge Assistant", false);
                        completeStage();
                    }
                }
                case ATTACK -> {
                    Module killFarm = ckpb1.client.CKPB1Client.modules().byName("Kill Farm");
                    if (killFarm == null || !killFarm.isEnabled() || inStage > 120_000) {
                        completeStage();
                    }
                }
                case DESTROY -> {
                    Module destroyer = ckpb1.client.CKPB1Client.modules().byName("Bed Destroyer V2");
                    if (destroyer == null || !destroyer.isEnabled() || inStage > 180_000) {
                        completeStage();
                    }
                }
                default -> {
                }
            }
        }

        StatusOverlay.set("Automation", List.of(
                "stage: §f" + stage,
                stage == Stage.COLLECT
                        ? resource.get() + ": " + countResource(resource.get()) + "/" + resourceAmount.getInt()
                        : String.format("%.0fs in stage", inStage / 1000.0)));
    }
}
