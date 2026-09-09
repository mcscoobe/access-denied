package com.osrs.accessdenied;

import java.awt.Color;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
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
	private final JCheckBox hideNpcsCheckbox = new JCheckBox("Hide NPCs");

	@Inject
	public AccessDeniedPanel(ConfigManager configManager)
	{
		JLabel title = new JLabel("Access Denied");
		title.setForeground(Color.WHITE);
		title.setFont(FontManager.getRunescapeBoldFont());
		add(title);

		hideNpcsCheckbox.setToolTipText("Hide all NPCs, along with their health bars, overhead prayers and hitsplats.");
		hideNpcsCheckbox.setForeground(Color.WHITE);
		hideNpcsCheckbox.setBackground(ColorScheme.DARK_GRAY_COLOR);
		// An ActionListener fires only on user interaction; setSelected() raises an ItemEvent
		// instead. Syncing the box from a config change therefore cannot loop back into
		// another config write.
		hideNpcsCheckbox.addActionListener(e -> configManager.setConfiguration(
			AccessDeniedConfig.CONFIG_GROUP,
			AccessDeniedConfig.HIDE_NPCS_KEY,
			hideNpcsCheckbox.isSelected()));
		add(hideNpcsCheckbox);
	}

	/**
	 * Reflects the stored value back into the checkbox. Called from config and profile
	 * changes, which do not necessarily arrive on the AWT event thread.
	 */
	void setHideNpcs(boolean hideNpcs)
	{
		SwingUtilities.invokeLater(() -> hideNpcsCheckbox.setSelected(hideNpcs));
	}
}
