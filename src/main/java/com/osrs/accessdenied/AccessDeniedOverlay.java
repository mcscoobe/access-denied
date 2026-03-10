package com.osrs.accessdenied;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;

/**
 * Overlay for highlighting boss entrances when validation fails.
 * Used specifically for CoX where blocking menu entries would prevent gear changes.
 */
@Slf4j
public class AccessDeniedOverlay extends Overlay
{
	private final Client client;
	private final AccessDeniedPlugin plugin;

	// Highlight color for failed validation
	private static final Color HIGHLIGHT_COLOR = new Color(255, 0, 0, 100); // Red with transparency
	private static final Color OUTLINE_COLOR = new Color(255, 0, 0, 255); // Solid red
	private static final Stroke OUTLINE_STROKE = new BasicStroke(2);

	@Inject
	public AccessDeniedOverlay(Client client, AccessDeniedPlugin plugin)
	{
		this.client = client;
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		// Only render if we're in a location that needs highlighting
		BossLocation currentLocation = plugin.getCurrentLocation();
		if (currentLocation == null)
		{
			return null;
		}

		// Only highlight for CoX
		if (!"cox".equals(currentLocation.getId()))
		{
			return null;
		}

		// Check if validation is required and failed
		ValidationResult validationResult = plugin.getPublicCachedValidationResult();
		if (validationResult == null || validationResult.isValid())
		{
			return null;
		}

		// Find and highlight the CoX entrance object
		Integer objectId = BossLocations.getObjectForLocation(currentLocation);
		if (objectId == null)
		{
			return null;
		}

		// Search for the entrance object in the scene
		Tile[][][] tiles = client.getScene().getTiles();
		int plane = client.getPlane();
		
		for (int x = 0; x < tiles[plane].length; x++)
		{
			for (int y = 0; y < tiles[plane][x].length; y++)
			{
				Tile tile = tiles[plane][x][y];
				if (tile == null)
				{
					continue;
				}

				GameObject[] gameObjects = tile.getGameObjects();
				if (gameObjects == null)
				{
					continue;
				}

				for (GameObject gameObject : gameObjects)
				{
					if (gameObject != null && gameObject.getId() == objectId)
					{
						highlightGameObject(graphics, gameObject);
					}
				}
			}
		}

		return null;
	}

	/**
	 * Highlight a game object with a red outline and fill.
	 */
	private void highlightGameObject(Graphics2D graphics, GameObject gameObject)
	{
		LocalPoint localPoint = gameObject.getLocalLocation();
		if (localPoint == null)
		{
			return;
		}

		// Draw a polygon at the object's location
		Polygon polygon = Perspective.getCanvasTilePoly(client, localPoint);
		if (polygon != null)
		{
			// Draw filled highlight
			graphics.setColor(HIGHLIGHT_COLOR);
			graphics.fill(polygon);
			
			// Draw outline
			graphics.setColor(OUTLINE_COLOR);
			graphics.setStroke(OUTLINE_STROKE);
			graphics.draw(polygon);
		}
	}
}
