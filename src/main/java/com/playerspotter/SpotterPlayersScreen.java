package com.playerspotter;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Type a player name and press Add. Only the players in this list get glow,
 * name, meter and arrow. While you type, matching players near you show up
 * as buttons (auto fill): click one to fill the name.
 */
public class SpotterPlayersScreen extends Screen {
	private static final int MAX_NAMES = 12;
	private static final int SUGGESTIONS = 3;

	private final Screen parent;
	private String typed = "";
	private TextFieldWidget input;
	private final ButtonWidget[] suggestButtons = new ButtonWidget[SUGGESTIONS];
	private final String[] suggestNames = new String[SUGGESTIONS];

	public SpotterPlayersScreen(Screen parent) {
		super(Text.literal("Choose players"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		SpotterConfig c = SpotterConfig.INSTANCE;
		int left = this.width / 2 - 155;
		int right = this.width / 2 + 5;

		input = new TextFieldWidget(this.textRenderer, left, 34, 200, 20, Text.literal("Player name"));
		input.setMaxLength(16);
		input.setPlaceholder(Text.literal("Type player name..."));
		input.setText(typed);
		input.setChangedListener(s -> {
			typed = s;
			refreshSuggestions();
		});
		addDrawableChild(input);
		setInitialFocus(input);

		addDrawableChild(ButtonWidget.builder(Text.literal("Add"), b -> addTyped())
			.dimensions(left + 205, 34, 105, 20).build());

		for (int i = 0; i < SUGGESTIONS; i++) {
			final int idx = i;
			suggestButtons[i] = ButtonWidget.builder(Text.empty(), b -> {
				if (suggestNames[idx] != null) {
					input.setText(suggestNames[idx]);
				}
			}).dimensions(left + i * 105, 58, 100, 20).build();
			suggestButtons[i].visible = false;
			addDrawableChild(suggestButtons[i]);
		}

		addDrawableChild(ButtonWidget.builder(onlyLabel(c.onlyListed), b -> {
			c.onlyListed = !c.onlyListed;
			b.setMessage(onlyLabel(c.onlyListed));
		}).dimensions(left, 84, 310, 20).build());

		// The saved list: click a name to remove it.
		int row = 0;
		for (int i = 0; i < c.players.size() && i < MAX_NAMES; i++) {
			final String name = c.players.get(i);
			int x = (i % 2 == 0) ? left : right;
			int y = 112 + (i / 2) * 22;
			addDrawableChild(ButtonWidget.builder(Text.literal("[x] " + name), b -> {
				c.players.removeIf(p -> p.equalsIgnoreCase(name));
				SpotterConfig.save();
				clearAndInit();
			}).dimensions(x, y, 150, 20).build());
			row = i / 2 + 1;
		}

		int doneY = Math.min(this.height - 28, 112 + Math.max(row, 1) * 22 + 12);
		addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
			.dimensions(this.width / 2 - 100, Math.max(doneY, 112 + row * 22 + 4), 200, 20).build());

		refreshSuggestions();
	}

	private static Text onlyLabel(boolean on) {
		return Text.literal("Only listed players: ")
			.append(Text.literal(on ? "ON" : "OFF (show everyone)").formatted(on ? Formatting.GREEN : Formatting.RED));
	}

	private void addTyped() {
		String name = typed.trim();
		if (name.isEmpty()) {
			return;
		}
		SpotterConfig c = SpotterConfig.INSTANCE;
		if (c.players.size() >= MAX_NAMES) {
			return;
		}
		for (String p : c.players) {
			if (p.equalsIgnoreCase(name)) {
				typed = "";
				clearAndInit();
				return;
			}
		}
		c.players.add(name);
		SpotterConfig.save();
		typed = "";
		clearAndInit();
	}

	/** Players near you whose name starts with what you typed. */
	private void refreshSuggestions() {
		for (int i = 0; i < SUGGESTIONS; i++) {
			suggestNames[i] = null;
			if (suggestButtons[i] != null) {
				suggestButtons[i].visible = false;
			}
		}
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.world == null || mc.player == null) {
			return;
		}
		String prefix = typed.trim().toLowerCase(Locale.ROOT);
		List<String> matches = new ArrayList<>();
		for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
			if (p == mc.player) {
				continue;
			}
			String n = p.getName().getString();
			if (n.toLowerCase(Locale.ROOT).startsWith(prefix)) {
				matches.add(n);
			}
		}
		for (int i = 0; i < SUGGESTIONS && i < matches.size(); i++) {
			suggestNames[i] = matches.get(i);
			if (suggestButtons[i] != null) {
				suggestButtons[i].setMessage(Text.literal(matches.get(i)));
				suggestButtons[i].visible = true;
			}
		}
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.render(ctx, mouseX, mouseY, delta);
		ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFFFF);
		if (SpotterConfig.INSTANCE.players.isEmpty()) {
			ctx.drawCenteredTextWithShadow(this.textRenderer,
				Text.literal("No players yet. Type a name and press Add."), this.width / 2, 118, 0xFFAAAAAA);
		}
	}

	@Override
	public void close() {
		this.client.setScreen(parent);
	}

	@Override
	public void removed() {
		SpotterConfig.save();
	}
}
