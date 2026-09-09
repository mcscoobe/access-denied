package com.osrs.accessdenied;

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

	private final JToggleButton hideNpcsToggle = new JToggleButton();

	@Inject
	public AccessDeniedPanel(ConfigManager configManager)
	{
		JLabel title = new JLabel("Access Denied");
		title.setForeground(Color.WHITE);
		title.setFont(FontManager.getRunescapeBoldFont());
		add(title);

		hideNpcsToggle.setFocusPainted(false);
		hideNpcsToggle.setToolTipText("Hide all NPCs, along with their health bars, overhead prayers and hitsplats.");
		// An ItemListener fires for a click and for setSelected alike, so the label always
		// matches the button. An ActionListener fires only on a click, so syncing the button
		// from a config change cannot loop back into another config write.
		hideNpcsToggle.addItemListener(e -> updateToggleAppearance());
		hideNpcsToggle.addActionListener(e -> configManager.setConfiguration(
			AccessDeniedConfig.CONFIG_GROUP,
			AccessDeniedConfig.HIDE_NPCS_KEY,
			hideNpcsToggle.isSelected()));
		updateToggleAppearance();
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
	private void updateToggleAppearance()
	{
		boolean hidden = hideNpcsToggle.isSelected();
		hideNpcsToggle.setText(hidden ? NPCS_HIDDEN : NPCS_VISIBLE);
		hideNpcsToggle.setBackground(hidden ? ColorScheme.BRAND_ORANGE : ColorScheme.DARKER_GRAY_COLOR);
		hideNpcsToggle.setForeground(hidden ? Color.BLACK : ColorScheme.LIGHT_GRAY_COLOR);
	}
}
