package com.example.slotbinding;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

@Mod(modid = SlotBindingMod.MODID, version = SlotBindingMod.VERSION, clientSideOnly = true)
public class SlotBindingMod {
    public static final String MODID = "slotbinding";
    public static final String VERSION = "1.0";
    public static final String PREFIX = "\u00A77[\u00A7bSlotBinding\u00A77] \u00A77> ";
    private static final String DEFAULT_PROFILE = "default";

    private static KeyBinding bindingKeybind;
    private static Map<Integer, Integer> slotBindings = new HashMap<Integer, Integer>();
    private static Map<Integer, List<Integer>> reverseBindings = new HashMap<Integer, List<Integer>>();
    private static Map<Integer, Integer> lastUsedSourceSlots = new HashMap<Integer, Integer>();
    private static Map<String, Map<Integer, Integer>> profiles = new HashMap<String, Map<Integer, Integer>>();
    private static String currentProfile = DEFAULT_PROFILE;
    private static Integer previousSlot = null;
    private static Configuration config;
    private static SlotBindingMod instance;
    private static Field guiLeftField;
    private static Field guiTopField;

    static {
        try {
            guiLeftField = GuiContainer.class.getDeclaredField("guiLeft");
            guiLeftField.setAccessible(true);
            
            guiTopField = GuiContainer.class.getDeclaredField("guiTop");
            guiTopField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            try {
                guiLeftField = GuiContainer.class.getDeclaredField("field_147003_i");
                guiLeftField.setAccessible(true);
                
                guiTopField = GuiContainer.class.getDeclaredField("field_147009_r");
                guiTopField.setAccessible(true);
            } catch (NoSuchFieldException ex) {
                ex.printStackTrace();
            }
        }
    }

    public SlotBindingMod() {
        instance = this;
    }

    public static SlotBindingMod getInstance() {
        return instance;
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        File configFile = new File(event.getModConfigurationDirectory(), "SlotBinding.cfg");
        config = new Configuration(configFile);
        loadConfig();
        
        bindingKeybind = new KeyBinding("Bind Slots", Keyboard.KEY_NONE, "SlotBinding");
        ClientRegistry.registerKeyBinding(bindingKeybind);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        ClientCommandHandler.instance.registerCommand(new SlotBindCommand());
    }

    public void sendChatMessage(String message) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        if (previousSlot != null && bindingKeybind.getKeyCode() != 0 && !Keyboard.isKeyDown(bindingKeybind.getKeyCode())) {
            previousSlot = null;
        }
    }

    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    @SubscribeEvent
    public void onMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!(event.gui instanceof GuiInventory)) {
            return;
        }
        
        if (!Mouse.getEventButtonState() || Mouse.getEventButton() != 0) {
            return;
        }
        
        GuiInventory gui = (GuiInventory) event.gui;
        Slot slotUnderMouse = getSlotUnderMouse(gui);
        
        if (slotUnderMouse == null || slotUnderMouse.slotNumber < 5) {
            return;
        }
        
        int slotNumber = slotUnderMouse.slotNumber;
        
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
            if (slotBindings.containsKey(slotNumber)) {
                event.setCanceled(true);
                handleShiftClick(slotNumber);
                return;
            }
            if (reverseBindings.containsKey(slotNumber)) {
                event.setCanceled(true);
                handleReverseShiftClick(slotNumber);
                return;
            }
            return;
        }
        
        if (bindingKeybind.getKeyCode() != 0 && !Keyboard.isKeyDown(bindingKeybind.getKeyCode())) {
            return;
        }
        
        event.setCanceled(true);
        
        // Check if clicking on an already bound slot
        if (previousSlot == null && slotBindings.containsKey(slotNumber)) {
            // Remove the binding
            Integer targetSlot = slotBindings.get(slotNumber);
            slotBindings.remove(slotNumber);
            updateReverseBindings();
            instance.saveConfig();
            sendChatMessage(PREFIX + "\u00A7aRemoved binding\u00A7r: \u00A76" + slotNumber + " \u00A7b-> \u00A76" + targetSlot);
            return;
        }
        
        if (previousSlot == null) {
            previousSlot = slotNumber;
            sendChatMessage(PREFIX + "\u00A7aSelected first slot: \u00A76" + slotNumber);
            return;
        }
        
        if (previousSlot != null && (slotNumber < 36 || slotNumber > 44)) {
            sendChatMessage(PREFIX + "\u00A7cPlease click a valid hotbar slot!");
            previousSlot = null;
            return;
        }
        
        if (slotNumber == previousSlot) {
            previousSlot = null;
            return;
        }
        
        slotBindings.put(previousSlot, slotNumber);
        updateReverseBindings();
        sendChatMessage(PREFIX + "\u00A7aSaved binding\u00A7r: \u00A76" + previousSlot + " \u00A7b-> \u00A76" + slotNumber);
        saveConfig();
        previousSlot = null;
    }

    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    private void handleShiftClick(int slotClicked) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        Container container = player.openContainer;
        Integer targetSlot = slotBindings.get(slotClicked);
        
        if (targetSlot == null) {
            return;
        }
        
        int hotbarSlot = targetSlot % 36;
        
        if (hotbarSlot >= 9) {
            return;
        }
        
        lastUsedSourceSlots.put(targetSlot, slotClicked);
        
        mc.playerController.windowClick(container.windowId, slotClicked, hotbarSlot, 2, player);
    }

    private void handleReverseShiftClick(int hotbarSlot) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        Container container = player.openContainer;
        
        List<Integer> sourceSlots = reverseBindings.get(hotbarSlot);
        if (sourceSlots == null || sourceSlots.isEmpty()) {
            return;
        }
        
        int actualHotbarSlot = hotbarSlot % 36;
        if (actualHotbarSlot >= 9) {
            return;
        }
        
        Integer lastUsedSlot = lastUsedSourceSlots.get(hotbarSlot);
        if (lastUsedSlot != null && sourceSlots.contains(lastUsedSlot)) {
            mc.playerController.windowClick(container.windowId, lastUsedSlot, actualHotbarSlot, 2, player);
            return;
        }
        
        Integer targetSlot = sourceSlots.get(sourceSlots.size() - 1);
        if (targetSlot != null) {
            lastUsedSourceSlots.put(hotbarSlot, targetSlot);
            mc.playerController.windowClick(container.windowId, targetSlot, actualHotbarSlot, 2, player);
        }
    }

    private static void updateReverseBindings() {
        reverseBindings.clear();
        for (Map.Entry<Integer, Integer> entry : slotBindings.entrySet()) {
            int source = entry.getKey();
            int target = entry.getValue();
            if (!reverseBindings.containsKey(target)) {
                reverseBindings.put(target, new ArrayList<Integer>());
            }
            reverseBindings.get(target).add(source);
        }
    }

    private static void switchProfile(String profileName) {
        if (!profiles.containsKey(profileName)) {
            profiles.put(profileName, new HashMap<Integer, Integer>());
        }
        currentProfile = profileName;
        slotBindings = profiles.get(profileName);
        updateReverseBindings();
        lastUsedSourceSlots.clear();
        instance.saveConfig();
    }

    private static void deleteProfile(String profileName) {
        if (profileName.equals(DEFAULT_PROFILE)) {
            return;
        }
        profiles.remove(profileName);
        if (currentProfile.equals(profileName)) {
            switchProfile(DEFAULT_PROFILE);
        }
        instance.saveConfig();
    }

    private static void clearAllBindings() {
        slotBindings.clear();
        reverseBindings.clear();
        lastUsedSourceSlots.clear();
        instance.saveConfig();
    }
    @SubscribeEvent
    public void onGuiDraw(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (!(event.gui instanceof GuiInventory)) {
            return;
        }
        
        GuiContainer gui = (GuiContainer) event.gui;
        
        int guiLeft = 0;
        int guiTop = 0;
        
        try {
            guiLeft = guiLeftField.getInt(gui);
            guiTop = guiTopField.getInt(gui);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        
        Slot hoveredSlot = getSlotUnderMouse(gui);
        
        // Save GL state before drawing
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        
        // Draw binding indicators
        for (Slot slot : gui.inventorySlots.inventorySlots) {
            if (slot.slotNumber < 5) continue;
            drawBindingIndicator(gui, slot, guiLeft, guiTop, hoveredSlot);
        }
        
        // Restore GL state after drawing
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    private void drawBindingIndicator(GuiContainer gui, Slot slot, int guiLeft, int guiTop, Slot hoveredSlot) {
        int x = guiLeft + slot.xDisplayPosition;
        int y = guiTop + slot.yDisplayPosition;
        
        boolean isSource = slotBindings.containsKey(slot.slotNumber);
        boolean isTarget = reverseBindings.containsKey(slot.slotNumber);
        
        if (isSource || isTarget) {
            // Save OpenGL state
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            
            int color = isSource ? 0xFF00FF00 : 0xFFFFAA00;
            
            // Draw the box outline
            drawRectangle(x, y, x + 16, y + 1, color); // Top
            drawRectangle(x, y, x + 1, y + 16, color); // Left
            drawRectangle(x + 15, y, x + 16, y + 16, color); // Right
            drawRectangle(x, y + 15, x + 16, y + 16, color); // Bottom
            
            // Only draw lines for the specific slot being hovered
            if (hoveredSlot != null && hoveredSlot.slotNumber == slot.slotNumber) {
                if (isSource) {
                    // Draw line from source to its target
                    Integer targetSlot = slotBindings.get(slot.slotNumber);
                    drawLineToSlot(gui, slot, targetSlot, guiLeft, guiTop, color);
                } else if (isTarget) {
                    // Draw line from most relevant source to this target
                    List<Integer> sources = reverseBindings.get(slot.slotNumber);
                    if (sources != null && !sources.isEmpty()) {
                        Integer sourceToUse = lastUsedSourceSlots.get(slot.slotNumber);
                        if (sourceToUse == null || !sources.contains(sourceToUse)) {
                            sourceToUse = sources.get(sources.size() - 1);
                        }
                        drawLineToSlot(gui, getSlotByNumber(gui, sourceToUse), slot.slotNumber, guiLeft, guiTop, color);
                    }
                }
            }
            
            // Restore OpenGL state
            GL11.glPopAttrib();
        }
    }
    
    private void drawRectangle(int left, int top, int right, int bottom, int color) {
        float alpha = ((color >> 24) & 0xFF) / 255.0F;
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        
        GL11.glColor4f(red, green, blue, alpha);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(left, top);
        GL11.glVertex2f(left, bottom);
        GL11.glVertex2f(right, bottom);
        GL11.glVertex2f(right, top);
        GL11.glEnd();
    }
    
    private void drawLineToSlot(GuiContainer gui, Slot fromSlot, int toSlotNumber, int guiLeft, int guiTop, int color) {
        Slot toSlot = getSlotByNumber(gui, toSlotNumber);
        if (fromSlot != null && toSlot != null) {
            int x1 = guiLeft + fromSlot.xDisplayPosition + 8;
            int y1 = guiTop + fromSlot.yDisplayPosition + 8;
            int x2 = guiLeft + toSlot.xDisplayPosition + 8;
            int y2 = guiTop + toSlot.yDisplayPosition + 8;
            
            float red = ((color >> 16) & 0xFF) / 255.0F;
            float green = ((color >> 8) & 0xFF) / 255.0F;
            float blue = (color & 0xFF) / 255.0F;
            float alpha = ((color >> 24) & 0xFF) / 255.0F;
            
            GL11.glLineWidth(2F);
            GL11.glColor4f(red, green, blue, alpha);
            GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex2i(x1, y1);
            GL11.glVertex2i(x2, y2);
            GL11.glEnd();
        }
    }

    private Slot getSlotByNumber(GuiContainer gui, int slotNumber) {
        for (Slot slot : gui.inventorySlots.inventorySlots) {
            if (slot.slotNumber == slotNumber) {
                return slot;
            }
        }
        return null;
    }

    private Slot getSlotUnderMouse(GuiContainer gui) {
        int mouseX = Mouse.getX() * gui.width / Minecraft.getMinecraft().displayWidth;
        int mouseY = gui.height - Mouse.getY() * gui.height / Minecraft.getMinecraft().displayHeight - 1;
        
        int guiLeft = 0;
        int guiTop = 0;
        
        try {
            guiLeft = guiLeftField.getInt(gui);
            guiTop = guiTopField.getInt(gui);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        
        for (int i = 0; i < gui.inventorySlots.inventorySlots.size(); i++) {
            Slot slot = (Slot) gui.inventorySlots.inventorySlots.get(i);
            if (isPointInRegion(guiLeft, guiTop, slot.xDisplayPosition, slot.yDisplayPosition, 16, 16, mouseX, mouseY)) {
                return slot;
            }
        }
        
        return null;
    }
    
    private boolean isPointInRegion(int guiLeft, int guiTop, int rectX, int rectY, int rectWidth, int rectHeight, int pointX, int pointY) {
        int i = guiLeft;
        int j = guiTop;
        pointX = pointX - i;
        pointY = pointY - j;
        return pointX >= rectX - 1 && pointX < rectX + rectWidth + 1 && pointY >= rectY - 1 && pointY < rectY + rectHeight + 1;
    }
    private void loadConfig() {
        profiles.clear();
        profiles.put(DEFAULT_PROFILE, new HashMap<Integer, Integer>());
        
        String[] profileNames = config.getStringList("profiles", Configuration.CATEGORY_GENERAL, new String[]{DEFAULT_PROFILE}, "Available profiles");
        currentProfile = config.getString("currentProfile", Configuration.CATEGORY_GENERAL, DEFAULT_PROFILE, "Currently selected profile");
        
        for (String profile : profileNames) {
            String[] bindings = config.getStringList("bindings." + profile, Configuration.CATEGORY_GENERAL, new String[]{}, "Slot bindings for profile " + profile);
            Map<Integer, Integer> profileBindings = new HashMap<Integer, Integer>();
            
            for (String binding : bindings) {
                String[] parts = binding.split(":");
                if (parts.length == 2) {
                    try {
                        int source = Integer.parseInt(parts[0]);
                        int target = Integer.parseInt(parts[1]);
                        profileBindings.put(source, target);
                    } catch (NumberFormatException e) {
                        // Skip invalid entries
                    }
                }
            }
            
            profiles.put(profile, profileBindings);
        }
        
        if (!profiles.containsKey(currentProfile)) {
            currentProfile = DEFAULT_PROFILE;
        }
        slotBindings = profiles.get(currentProfile);
        updateReverseBindings();
        
        if (config.hasChanged()) {
            config.save();
        }
    }

    private void saveConfig() {
        config.get(Configuration.CATEGORY_GENERAL, "currentProfile", DEFAULT_PROFILE).set(currentProfile);
        
        String[] profileNames = profiles.keySet().toArray(new String[profiles.size()]);
        config.get(Configuration.CATEGORY_GENERAL, "profiles", new String[]{DEFAULT_PROFILE}).set(profileNames);
        
        for (Map.Entry<String, Map<Integer, Integer>> profile : profiles.entrySet()) {
            String profileName = profile.getKey();
            Map<Integer, Integer> profileBindings = profile.getValue();
            
            String[] bindings = new String[profileBindings.size()];
            int i = 0;
            for (Map.Entry<Integer, Integer> entry : profileBindings.entrySet()) {
                bindings[i++] = entry.getKey() + ":" + entry.getValue();
            }
            
            config.get(Configuration.CATEGORY_GENERAL, "bindings." + profileName, new String[]{}).set(bindings);
        }
        
        if (config.hasChanged()) {
            config.save();
        }
    }

    public static class SlotBindCommand extends net.minecraft.command.CommandBase {
        private static final String USAGE = "/slotbind <delete|profile> [args...]";
        private static final String PROFILE_USAGE = "/slotbind profile <list|switch|create|delete> [profile]";

        @Override
        public String getCommandName() {
            return "slotbind";
        }

        @Override
        public String getCommandUsage(net.minecraft.command.ICommandSender sender) {
            return USAGE;
        }

        @Override
        public void processCommand(net.minecraft.command.ICommandSender sender, String[] args) {
            if (args.length == 0) {
                instance.sendChatMessage(PREFIX + "\u00A7cUsage: " + USAGE);
                return;
            }

            String subCommand = args[0].toLowerCase();
            
            if ("delete".equals(subCommand)) {
                handleDeleteCommand(sender, Arrays.copyOfRange(args, 1, args.length));
            } else if ("profile".equals(subCommand)) {
                handleProfileCommand(sender, Arrays.copyOfRange(args, 1, args.length));
            } else {
                instance.sendChatMessage(PREFIX + "\u00A7cUnknown subcommand: " + subCommand);
                instance.sendChatMessage(PREFIX + "\u00A7cUsage: " + USAGE);
            }
        }
        private void handleDeleteCommand(net.minecraft.command.ICommandSender sender, String[] args) {
            if (args.length == 0) {
                if (slotBindings.isEmpty()) {
                    instance.sendChatMessage(PREFIX + "\u00A7cNo bindings set yet!");
                    return;
                }
                
                StringBuilder slots = new StringBuilder();
                boolean first = true;
                
                for (Integer slot : slotBindings.keySet()) {
                    if (!first) {
                        slots.append(", ");
                    }
                    slots.append("\u00A7b").append(slot);
                    first = false;
                }
                
                instance.sendChatMessage(PREFIX + "\u00A7cPlease set a valid slot or 'all'! \u00A77slots\u00A7r: " + slots.toString());
                return;
            }
            
            if (args[0].equalsIgnoreCase("all")) {
                if (slotBindings.isEmpty()) {
                    instance.sendChatMessage(PREFIX + "\u00A7cNo bindings to delete!");
                    return;
                }
                clearAllBindings();
                instance.sendChatMessage(PREFIX + "\u00A7aAll bindings have been deleted!");
                return;
            }
            
            try {
                int slot = Integer.parseInt(args[0]);
                if (!slotBindings.containsKey(slot)) {
                    instance.sendChatMessage(PREFIX + "\u00A7cNo binding exists for slot " + slot);
                    return;
                }
                
                slotBindings.remove(slot);
                updateReverseBindings();
                instance.saveConfig();
                instance.sendChatMessage(PREFIX + "\u00A7aBinding with slot \u00A7b" + slot + " \u00A7adeleted");
            } catch (NumberFormatException e) {
                instance.sendChatMessage(PREFIX + "\u00A7cPlease provide a valid slot number or 'all'!");
            }
        }

        private void handleProfileCommand(net.minecraft.command.ICommandSender sender, String[] args) {
            if (args.length == 0) {
                instance.sendChatMessage(PREFIX + "\u00A7cUsage: " + PROFILE_USAGE);
                return;
            }

            String action = args[0].toLowerCase();
            if ("list".equals(action)) {
                handleProfileList();
            } else if ("switch".equals(action)) {
                handleProfileSwitch(args);
            } else if ("create".equals(action)) {
                handleProfileCreate(args);
            } else if ("delete".equals(action)) {
                handleProfileDelete(args);
            } else {
                instance.sendChatMessage(PREFIX + "\u00A7cUnknown profile action: " + action);
                instance.sendChatMessage(PREFIX + "\u00A7cUsage: " + PROFILE_USAGE);
            }
        }

        private void handleProfileList() {
            StringBuilder profileList = new StringBuilder();
            boolean first = true;
            for (String profile : profiles.keySet()) {
                if (!first) {
                    profileList.append(", ");
                }
                if (profile.equals(currentProfile)) {
                    profileList.append("\u00A7a").append(profile).append("\u00A7r");
                } else {
                    profileList.append("\u00A7b").append(profile).append("\u00A7r");
                }
                first = false;
            }
            instance.sendChatMessage(PREFIX + "\u00A77Profiles: " + profileList.toString());
        }

        private void handleProfileSwitch(String[] args) {
            if (args.length < 2) {
                instance.sendChatMessage(PREFIX + "\u00A7cPlease specify a profile name!");
                return;
            }
            String profileToSwitch = args[1];
            if (!profiles.containsKey(profileToSwitch)) {
                instance.sendChatMessage(PREFIX + "\u00A7cProfile '" + profileToSwitch + "' does not exist!");
                return;
            }
            switchProfile(profileToSwitch);
            instance.sendChatMessage(PREFIX + "\u00A7aSwitched to profile: \u00A7b" + profileToSwitch);
        }

        private void handleProfileCreate(String[] args) {
            if (args.length < 2) {
                instance.sendChatMessage(PREFIX + "\u00A7cPlease specify a profile name!");
                return;
            }
            String newProfile = args[1];
            if (profiles.containsKey(newProfile)) {
                instance.sendChatMessage(PREFIX + "\u00A7cProfile '" + newProfile + "' already exists!");
                return;
            }
            profiles.put(newProfile, new HashMap<Integer, Integer>());
            instance.saveConfig();
            instance.sendChatMessage(PREFIX + "\u00A7aCreated new profile: \u00A7b" + newProfile);
        }

        private void handleProfileDelete(String[] args) {
            if (args.length < 2) {
                instance.sendChatMessage(PREFIX + "\u00A7cPlease specify a profile name!");
                return;
            }
            String profileToDelete = args[1];
            if (profileToDelete.equals(DEFAULT_PROFILE)) {
                instance.sendChatMessage(PREFIX + "\u00A7cCannot delete the default profile!");
                return;
            }
            if (!profiles.containsKey(profileToDelete)) {
                instance.sendChatMessage(PREFIX + "\u00A7cProfile '" + profileToDelete + "' does not exist!");
                return;
            }
            deleteProfile(profileToDelete);
            instance.sendChatMessage(PREFIX + "\u00A7aDeleted profile: \u00A7b" + profileToDelete);
        }

        @Override
        public List<String> addTabCompletionOptions(net.minecraft.command.ICommandSender sender, String[] args, net.minecraft.util.BlockPos pos) {
            List<String> options = new ArrayList<String>();

            if (args.length == 1) {
                options.add("delete");
                options.add("profile");
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("delete")) {
                    options.add("all");
                    for (Integer slot : slotBindings.keySet()) {
                        options.add(String.valueOf(slot));
                    }
                } else if (args[0].equalsIgnoreCase("profile")) {
                    options.add("list");
                    options.add("switch");
                    options.add("create");
                    options.add("delete");
                }
            } else if (args.length == 3) {
                if (args[0].equalsIgnoreCase("profile")) {
                    if (args[1].equalsIgnoreCase("switch") || args[1].equalsIgnoreCase("delete")) {
                        options.addAll(profiles.keySet());
                    }
                }
            }
            
            return options;
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }

        @Override
        public boolean canCommandSenderUseCommand(net.minecraft.command.ICommandSender sender) {
            return true;
        }
    }
}