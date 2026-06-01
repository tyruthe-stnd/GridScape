package com.gridscape.task.ui;

import com.gridscape.task.TaskTile;
import com.gridscape.util.FogTileCompositor;
import com.gridscape.util.GridClaimFocusAnimation;
import com.gridscape.util.GridScapeSwingUtil;
import com.gridscape.util.ScaledImageCache;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Window;
import java.awt.image.BufferedImage;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import net.runelite.api.Client;
import net.runelite.client.util.ImageUtil;

/**
 * Shared task-grid cell UI: fitted icon panels and claimed cells used by the global task panel,
 * world map task popup, and (for icon fitting) world unlock grid.
 */
public final class TaskTileCellFactory
{
	public static final int CLAIMED_CHECKMARK_SIZE = 18;
	public static final int CLAIMED_CHECKMARK_INSET = 4;
	public static final Color TASK_TILE_FALLBACK_BG = new Color(60, 55, 50);
	public static final Color CLAIMED_DESEATURATE_OVERLAY = new Color(120, 120, 120, 140);

	public static final class DetailPopupShell
	{
		public final JDialog detail;
		public final JPanel body;

		DetailPopupShell(JDialog detail, JPanel body)
		{
			this.detail = detail;
			this.body = body;
		}
	}

	private TaskTileCellFactory()
	{
	}

	/** Tile background plus a single fitted icon (world unlock revealed-unclaimed cells). */
	public static void paintBackgroundAndFittedIcon(Graphics g, int cw, int ch, BufferedImage tileBg, BufferedImage iconImage, int margin)
	{
		if (tileBg != null)
		{
			ScaledImageCache.drawScaled(g, tileBg, 0, 0, cw, ch);
		}
		else
		{
			g.setColor(TASK_TILE_FALLBACK_BG);
			g.fillRect(0, 0, cw, ch);
		}
		if (iconImage != null)
		{
			paintIconFittedInMargin(g, iconImage, margin, cw, ch);
		}
	}

	/** Paints scaled tile background and optional center icon; pass component width/height. */
	public static void paintBackgroundAndCenterIcon(Graphics g, int cw, int ch, BufferedImage tileBg, BufferedImage centerIcon, boolean centerTile)
	{
		if (tileBg != null)
		{
			ScaledImageCache.drawScaled(g, tileBg, 0, 0, cw, ch);
		}
		else
		{
			g.setColor(TASK_TILE_FALLBACK_BG);
			g.fillRect(0, 0, cw, ch);
		}
		if (centerTile && centerIcon != null)
		{
			int size = Math.min(cw, ch) * 3 / 4;
			int x = (cw - size) / 2;
			int y = (ch - size) / 2;
			ScaledImageCache.drawScaled(g, centerIcon, x, y, size, size);
		}
	}

	/** Paints a task icon scaled to fit inside margins (same math as legacy task cells). */
	public static void paintIconFittedInMargin(Graphics g, BufferedImage iconImage, int margin, int cw, int ch)
	{
		if (iconImage == null) return;
		int innerW = Math.max(1, cw - 2 * margin);
		int innerH = Math.max(1, ch - 2 * margin);
		int iw = iconImage.getWidth();
		int ih = iconImage.getHeight();
		if (iw <= 0 || ih <= 0) return;
		double scale = Math.min((double) innerW / iw, (double) innerH / ih);
		int drawW = Math.max(1, (int) Math.round(iw * scale));
		int drawH = Math.max(1, (int) Math.round(ih * scale));
		int x = margin + (innerW - drawW) / 2;
		int y = margin + (innerH - drawH) / 2;
		ScaledImageCache.drawScaled(g, iconImage, x, y, drawW, drawH);
	}

	/**
	 * Icon area for a task tile: draws the icon fitted with margins. Caller adds to BorderLayout.CENTER and wires listeners.
	 */
	public static JPanel newFittedTaskIconPanel(BufferedImage iconImage, int margin)
	{
		JPanel iconPanel = new JPanel()
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				super.paintComponent(g);
				paintIconFittedInMargin(g, iconImage, margin, getWidth(), getHeight());
			}
		};
		iconPanel.setOpaque(false);
		return iconPanel;
	}

	/** Unclaimed task cell shell: tile background, optional center icon, optional fitted task icon. Caller wires listeners. */
	public static JPanel newActiveTaskCell(int tileSize, BufferedImage tileBg, BufferedImage centerIcon,
		boolean isCenter, BufferedImage taskIcon, int iconMargin)
	{
		final BufferedImage tileBgFinal = tileBg;
		final BufferedImage centerIconFinal = centerIcon;
		final boolean centerTile = isCenter;
		JPanel cell = new JPanel()
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				paintBackgroundAndCenterIcon(g, getWidth(), getHeight(), tileBgFinal, centerIconFinal, centerTile);
				super.paintComponent(g);
			}
		};
		cell.setLayout(new BorderLayout());
		cell.setOpaque(false);
		cell.setPreferredSize(new Dimension(tileSize, tileSize));
		if (taskIcon != null && !centerTile)
			cell.add(newFittedTaskIconPanel(taskIcon, iconMargin), BorderLayout.CENTER);
		return cell;
	}

	/** Non-interactive frontier fog cell with optional edge art toward revealed-unclaimed neighbors. */
	public static JPanel newFogCell(int row, int col, int tileSize,
		BufferedImage fogBg, BufferedImage ftl, BufferedImage ftr, BufferedImage fbl, BufferedImage fbr,
		boolean[] cardinalFlags)
	{
		final BufferedImage bg = fogBg;
		JPanel cell = new JPanel()
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				super.paintComponent(g);
				if (bg != null)
					ScaledImageCache.drawScaled(g, bg, 0, 0, getWidth(), getHeight());
				else
				{
					g.setColor(TASK_TILE_FALLBACK_BG);
					g.fillRect(0, 0, getWidth(), getHeight());
				}
				FogTileCompositor.paintFogQuadrants(g, getWidth(), getHeight(),
					cardinalFlags[0], cardinalFlags[1], cardinalFlags[2], cardinalFlags[3],
					ftl, ftr, fbl, fbr);
			}
		};
		cell.setOpaque(false);
		cell.setPreferredSize(new Dimension(tileSize, tileSize));
		cell.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
		GridClaimFocusAnimation.putGridCellKeys(cell, row, col);
		return cell;
	}

	/**
	 * Claimed cell for area/global task grids: gray overlay + checkmark (center or corner).
	 */
	public static JPanel newClaimedTaskCellForTaskGrid(int tileSize, BufferedImage tileBg, BufferedImage checkmarkImg, BufferedImage centerTileIcon, boolean isCenter)
	{
		final BufferedImage bg = tileBg;
		final BufferedImage checkmark = checkmarkImg != null
			? ImageUtil.resizeImage(checkmarkImg, CLAIMED_CHECKMARK_SIZE, CLAIMED_CHECKMARK_SIZE) : null;
		final BufferedImage centerIcon = centerTileIcon;
		final boolean centerTile = isCenter;

		JPanel cell = new JPanel()
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				if (bg != null)
				{
					ScaledImageCache.drawScaled(g, bg, 0, 0, getWidth(), getHeight());
				}
				else
				{
					g.setColor(TASK_TILE_FALLBACK_BG);
					g.fillRect(0, 0, getWidth(), getHeight());
				}
				if (centerTile && centerIcon != null)
				{
					int w = getWidth();
					int h = getHeight();
					int size = Math.min(w, h) * 3 / 4;
					int x = (w - size) / 2;
					int y = (h - size) / 2;
					ScaledImageCache.drawScaled(g, centerIcon, x, y, size, size);
				}
				g.setColor(CLAIMED_DESEATURATE_OVERLAY);
				g.fillRect(0, 0, getWidth(), getHeight());
				if (checkmark != null)
				{
					if (centerTile)
					{
						int x = (getWidth() - CLAIMED_CHECKMARK_SIZE) / 2;
						int y = (getHeight() - CLAIMED_CHECKMARK_SIZE) / 2;
						g.drawImage(checkmark, x, y, null);
					}
					else
					{
						g.drawImage(checkmark, getWidth() - CLAIMED_CHECKMARK_SIZE - CLAIMED_CHECKMARK_INSET,
							CLAIMED_CHECKMARK_INSET, null);
					}
				}
			}
		};
		cell.setOpaque(false);
		cell.setPreferredSize(new Dimension(tileSize, tileSize));
		cell.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
		return cell;
	}

	public static Frame resolveDialogOwner(Window parentWindow, Client client)
	{
		if (parentWindow != null)
		{
			Window w = parentWindow instanceof JDialog ? ((JDialog) parentWindow).getOwner() : parentWindow.getOwner();
			if (w == null && parentWindow instanceof Frame)
				w = parentWindow;
			if (w instanceof Frame)
				return (Frame) w;
		}
		if (client != null)
		{
			Window w = SwingUtilities.windowForComponent(client.getCanvas());
			if (w instanceof Frame)
				return (Frame) w;
		}
		return null;
	}

	public static DetailPopupShell newDetailPopupShell(Frame owner, String title, Color popupBg, Color popupBorder,
		Color textColor, BufferedImage xBtnImg, Runnable onClose)
	{
		JDialog detail = new JDialog(owner, title, false);
		detail.setUndecorated(true);

		JPanel content = new JPanel(new BorderLayout(8, 8));
		content.setBackground(popupBg);
		content.setBorder(new CompoundBorder(
			new LineBorder(popupBorder, 2),
			new EmptyBorder(12, 14, 12, 14)));

		JPanel headerPanel = new JPanel(new BorderLayout(4, 0));
		headerPanel.setOpaque(false);
		JLabel titleLabel = new JLabel(title);
		titleLabel.setForeground(textColor);
		titleLabel.setFont(titleLabel.getFont().deriveFont(java.awt.Font.BOLD, 13f));
		headerPanel.add(titleLabel, BorderLayout.CENTER);
		JButton closeBtn = GridScapeSwingUtil.newPopupButtonWithIcon(xBtnImg, textColor);
		closeBtn.addActionListener(e -> {
			if (onClose != null) onClose.run();
			detail.dispose();
		});
		headerPanel.add(closeBtn, BorderLayout.EAST);
		content.add(headerPanel, BorderLayout.NORTH);

		JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setOpaque(false);
		content.add(body, BorderLayout.CENTER);

		detail.setContentPane(content);
		detail.getRootPane().setBorder(new LineBorder(popupBorder, 2));
		return new DetailPopupShell(detail, body);
	}

	public static void addTierPointsRow(JPanel body, TaskTile tile, Color textColor)
	{
		JLabel detailsLabel = new JLabel("<html>Tier " + tile.getTier() + " &middot; " + tile.getPoints()
			+ " point" + (tile.getPoints() != 1 ? "s" : "") + "</html>");
		detailsLabel.setForeground(textColor);
		body.add(detailsLabel);
		body.add(new JLabel(" "));
	}

	public static void installDetailPopupFocusClose(JDialog detail)
	{
		detail.addWindowFocusListener(new java.awt.event.WindowAdapter()
		{
			@Override
			public void windowLostFocus(java.awt.event.WindowEvent e)
			{
				SwingUtilities.invokeLater(() -> {
					if (detail.isDisplayable()) detail.dispose();
				});
			}
		});
	}

	public static void showDetailPopup(JDialog detail, Component locationRelativeTo)
	{
		detail.pack();
		if (locationRelativeTo != null)
			detail.setLocationRelativeTo(locationRelativeTo);
		detail.setVisible(true);
		detail.requestFocusInWindow();
	}
}
