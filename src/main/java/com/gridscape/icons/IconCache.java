package com.gridscape.icons;

import com.gridscape.GridScapePlugin;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.runelite.client.util.ImageUtil;

/** Load and scale icons from plugin classpath resources. */
public final class IconCache
{
	private static final Map<String, BufferedImage> rawTaskIconCache = new ConcurrentHashMap<>();

	private IconCache() {}

	public static BufferedImage loadWithFallback(String primaryPath, String fallbackPath)
	{
		BufferedImage img = load(primaryPath);
		if (img != null) return img;
		return load(fallbackPath);
	}

	/**
	 * RuneLite {@link ImageUtil#loadImageResource(Class, String)} treats paths without a leading {@code '/'}
	 * as relative to the class's package ({@code com/gridscape/}), which breaks resources under
	 * {@code /com/taskIcons/}, {@code /com/bossicons/}, etc. Classpath-root resources must keep the leading slash.
	 */
	private static BufferedImage load(String path)
	{
		if (path == null || path.isEmpty()) return null;
		String p = path;
		if (!p.startsWith("/") && p.startsWith("com/"))
			p = "/" + p;
		try
		{
			return ImageUtil.loadImageResource(GridScapePlugin.class, p);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	public static BufferedImage loadRawTaskIcon(String taskType, String displayName, String bossId)
	{
		String path = IconResolver.resolveTaskTileLocalIconPath(taskType, displayName, bossId);
		if (path == null) return null;
		return rawTaskIconCache.computeIfAbsent(path, p -> loadWithFallback(p, IconResources.GENERIC_TASK_ICON));
	}

	public static BufferedImage loadDefaultTaskIcon()
	{
		return loadWithFallback(IconResources.GENERIC_TASK_ICON,
			IconResources.TASK_ICONS_RESOURCE_PREFIX + "Other_icon.png");
	}

	/** Largest dimension of Combat icon scaled to {@code iconMaxFit} (Quest/diary/collection log icon sizing). */
	public static int combatReferenceSize(int iconMaxFit)
	{
		BufferedImage combatRaw = loadRawTaskIcon("Combat", null, null);
		BufferedImage combatScaled = combatRaw != null ? scaleToFitAllowUpscale(combatRaw, iconMaxFit, iconMaxFit) : null;
		return combatScaled != null ? Math.max(combatScaled.getWidth(), combatScaled.getHeight()) : iconMaxFit;
	}

	public static BufferedImage scaleTaskIcon(BufferedImage raw, String taskType, String displayName,
		int iconMaxFit, int refSize)
	{
		if (raw == null) return null;
		return IconResolver.isIconMatchCombatSize(taskType, displayName)
			? scaleToLargestDimension(raw, refSize)
			: scaleToFitAllowUpscale(raw, iconMaxFit, iconMaxFit);
	}

	public static BufferedImage scaleToFitAllowUpscale(BufferedImage src, int maxW, int maxH)
	{
		if (src == null || maxW <= 0 || maxH <= 0) return null;
		int w = src.getWidth();
		int h = src.getHeight();
		if (w <= 0 || h <= 0) return null;
		double scale = Math.min((double) maxW / w, (double) maxH / h);
		int nw = Math.max(1, (int) Math.round(w * scale));
		int nh = Math.max(1, (int) Math.round(h * scale));
		return (nw == w && nh == h) ? src : ImageUtil.resizeImage(src, nw, nh);
	}

	public static BufferedImage scaleToLargestDimension(BufferedImage src, int targetMaxDimension)
	{
		if (src == null || targetMaxDimension <= 0) return null;
		int w = src.getWidth();
		int h = src.getHeight();
		if (w <= 0 || h <= 0) return null;
		int maxDim = Math.max(w, h);
		if (maxDim <= 0) return null;
		double scale = (double) targetMaxDimension / maxDim;
		int nw = Math.max(1, (int) Math.round(w * scale));
		int nh = Math.max(1, (int) Math.round(h * scale));
		return (nw == w && nh == h) ? src : ImageUtil.resizeImage(src, nw, nh);
	}
}
