package com.osrs.accessdenied;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Color;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * Sidebar panel for the plugin's in-the-moment toggles — the settings you flip mid-session
 * rather than configure once. Every control writes straight to the plugin's config group,
 * so the value persists and the plugin picks it up through its normal ConfigChanged path.
 */
@Singleton
public class AccessDeniedPanel extends PluginPanel
{
	private static final String NPCS_HIDDEN = "NPCs hidden";
	private static final String NPCS_VISIBLE = "NPCs visible";

	/**
	 * FlatLaf resolves a selected button's colours from the look and feel's
	 * ToggleButton.selected* defaults and never reads the component's own background or
	 * foreground, so plain setters go unused in that state. A style property is the supported
	 * way to override them per component; setting one also unshares this button's UI
	 * delegate, which keeps the override off every other toggle button in the client.
	 */
	private static final String SELECTED_STYLE = String.format(
		"selectedBackground: #%06X; selectedForeground: #%06X",
		ColorScheme.BRAND_ORANGE.getRGB() & 0xFFFFFF,
		Color.BLACK.getRGB() & 0xFFFFFF);

	private final JToggleButton hideNpcsToggle = new JToggleButton();

	@Inject
	public AccessDeniedPanel(ConfigManager configManager)
	{
		JLabel title = new JLabel("Access Denied");
		title.setForeground(Color.WHITE);
		title.setFont(FontManager.getRunescapeBoldFont());
		add(title);

		hideNpcsToggle.setFocusPainted(false);
		hideNpcsToggle.setToolTipText("Hide all NPCs, along with their health bars, overhead prayers and"
			+ " hitsplats. Hidden NPCs cannot be clicked.");
		// FlatLaf reads these two only while the button is unselected; the selected pair comes
		// from the style below.
		hideNpcsToggle.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		hideNpcsToggle.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		hideNpcsToggle.putClientProperty(FlatClientProperties.STYLE, SELECTED_STYLE);
		// An ItemListener fires for a click and for setSelected alike, so the label always
		// matches the button. An ActionListener fires only on a click, so syncing the button
		// from a config change cannot loop back into another config write.
		hideNpcsToggle.addItemListener(e -> updateToggleText());
		hideNpcsToggle.addActionListener(e -> configManager.setConfiguration(
			AccessDeniedConfig.CONFIG_GROUP,
			AccessDeniedConfig.HIDE_NPCS_KEY,
			hideNpcsToggle.isSelected()));
		updateToggleText();
		add(hideNpcsToggle);
	}

	/**
	 * Reflects the stored value back into the button. Called from config and profile changes,
	 * which do not necessarily arrive on the AWT event thread.
	 */
	void setHideNpcs(boolean hideNpcs)
	{
		SwingUtilities.invokeLater(() -> hideNpcsToggle.setSelected(hideNpcs));
	}

	/**
	 * The button states which way it is currently set rather than what clicking it will do.
	 * A checkmark is not legible here: at sidebar size it is a few dark pixels against the
	 * panel's dark grey, so the state has to be carried by the text and the colour.
	 */
	private void updateToggleText()
	{
		hideNpcsToggle.setText(hideNpcsToggle.isSelected() ? NPCS_HIDDEN : NPCS_VISIBLE);
	}
}
