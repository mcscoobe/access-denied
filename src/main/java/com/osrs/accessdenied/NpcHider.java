package com.osrs.accessdenied;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.NPC;
import net.runelite.api.Renderable;
import net.runelite.client.callback.RenderCallback;
import net.runelite.client.callback.RenderCallbackManager;

/**
 * Hides every NPC while enabled by vetoing them as the client assembles each frame.
 *
 * <p>This is the same mechanism the client's own Entity Hider plugin uses, so the two
 * coexist without interfering: the client hides an entity if any registered callback
 * vetoes it, and neither plugin can force one back into view.
 */
@Singleton
public class NpcHider implements RenderCallback
{
	private final RenderCallbackManager renderCallbackManager;

	/**
	 * Written from the AWT event thread when the side panel or a config change flips it,
	 * read from the client thread on every rendered entity.
	 */
	private volatile boolean hideNpcs;

	private boolean registered;

	@Inject
	public NpcHider(RenderCallbackManager renderCallbackManager)
	{
		this.renderCallbackManager = renderCallbackManager;
	}

	void startUp()
	{
		if (!registered)
		{
			renderCallbackManager.register(this);
			registered = true;
		}
	}

	void shutDown()
	{
		if (registered)
		{
			renderCallbackManager.unregister(this);
			registered = false;
		}

		hideNpcs = false;
	}

	void setHideNpcs(boolean hideNpcs)
	{
		this.hideNpcs = hideNpcs;
	}

	boolean isHideNpcs()
	{
		return hideNpcs;
	}

	/**
	 * {@code ui} separates the 3D model pass from the 2D overlay pass — health bars,
	 * overhead prayers, hitsplats and names. Both are vetoed, so a hidden NPC leaves
	 * nothing at all behind.
	 *
	 * <p>Vetoing the 3D pass also drops the NPC's clickbox, so a hidden NPC cannot be
	 * attacked or talked to. That is inherent to the callback and matches Entity Hider;
	 * the README warns players about it.
	 */
	@Override
	public boolean addEntity(Renderable renderable, boolean ui)
	{
		return !(hideNpcs && renderable instanceof NPC);
	}
}
