package com.osrs.accessdenied;

import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Projectile;
import net.runelite.api.Renderable;
import net.runelite.client.callback.RenderCallbackManager;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for the NPC render veto behind the side panel's Hide NPCs toggle.
 */
class NpcHiderTest
{
	@Mock
	private RenderCallbackManager renderCallbackManager;

	@Mock
	private NPC npc;

	@Mock
	private Player player;

	@Mock
	private Projectile projectile;

	private NpcHider npcHider;
	private AutoCloseable mocks;

	@BeforeTry
	void setUp()
	{
		mocks = MockitoAnnotations.openMocks(this);
		npcHider = new NpcHider(renderCallbackManager);
	}

	@AfterTry
	void tearDown() throws Exception
	{
		mocks.close();
	}

	@Example
	void testNpcsDrawWhileTheToggleIsOff()
	{
		assertThat(npcHider.isHideNpcs()).isFalse();
		assertThat(npcHider.addEntity(npc, false)).isTrue();
		assertThat(npcHider.addEntity(npc, true)).isTrue();
	}

	@Example
	void testHidesNpcModelAndOverlaysWhileTheToggleIsOn()
	{
		npcHider.setHideNpcs(true);

		// false is the 3D model pass, true the 2D overlay pass — health bars, overhead
		// prayers, hitsplats and names. Both must be vetoed for the NPC to fully disappear.
		assertThat(npcHider.addEntity(npc, false)).isFalse();
		assertThat(npcHider.addEntity(npc, true)).isFalse();
	}

	@Example
	void testNpcsReturnWhenTheToggleGoesBackOff()
	{
		npcHider.setHideNpcs(true);
		npcHider.setHideNpcs(false);

		assertThat(npcHider.addEntity(npc, false)).isTrue();
		assertThat(npcHider.addEntity(npc, true)).isTrue();
	}

	@Example
	void testEntitiesOtherThanNpcsAreNeverHidden()
	{
		npcHider.setHideNpcs(true);

		for (Renderable renderable : new Renderable[]{player, projectile, mock(Renderable.class)})
		{
			assertThat(npcHider.addEntity(renderable, false)).isTrue();
			assertThat(npcHider.addEntity(renderable, true)).isTrue();
		}
	}

	@Example
	void testStartUpRegistersAtMostOnce()
	{
		npcHider.startUp();
		npcHider.startUp();

		verify(renderCallbackManager, times(1)).register(npcHider);
	}

	@Example
	void testShutDownUnregistersAndClearsTheToggle()
	{
		npcHider.startUp();
		npcHider.setHideNpcs(true);

		npcHider.shutDown();

		verify(renderCallbackManager).unregister(npcHider);
		assertThat(npcHider.isHideNpcs()).isFalse();
	}

	@Example
	void testShutDownWithoutStartUpUnregistersNothing()
	{
		npcHider.shutDown();

		verifyNoInteractions(renderCallbackManager);
	}
}
