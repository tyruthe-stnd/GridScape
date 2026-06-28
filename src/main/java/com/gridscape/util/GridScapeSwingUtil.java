package com.gridscape.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.MouseMotionAdapter;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import com.gridscape.task.TaskTile;
import com.gridscape.worldunlock.GlobalTaskListService;
import com.gridscape.worldunlock.GlobalTaskBookmark;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import net.runelite.api.Client;
import net.runelite.client.util.ImageUtil;
import com.gridscape.task.ui.TaskTileCellFactory;

import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.RootPaneContainer;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Shared Swing helpers for GridScape: scrollable panel, collapsible section,
 * JSON save dialog, popup-style buttons, and Escape-to-close for windows.
 */
public final class GridScapeSwingUtil
{
	/** Used by popup-style buttons for pressed state; exposed for custom buttons that share the same look. */
	public static final Color PRESSED_INSET_SHADOW = new Color(0, 0, 0, 70);
	public static final int PRESSED_INSET = 2;
	public static final Dimension RECTANGLE_BUTTON_SIZE = new Dimension(160, 28);

	private GridScapeSwingUtil() {}

	/** Panel that tracks viewport width in a scroll pane (no horizontal scroll). */
	public static final class ScrollableWidthPanel extends JPanel implements Scrollable
	{
		@Override
		public boolean getScrollableTracksViewportWidth() { return true; }
		@Override
		public boolean getScrollableTracksViewportHeight() { return false; }
		@Override
		public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 10; }
		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return visibleRect.height; }
	}

	/** Creates a new ScrollableWidthPanel. */
	public static JPanel newScrollableTrackWidthPanel()
	{
		return new ScrollableWidthPanel();
	}

	/**
	 * Builds a collapsible section: header (title + ▼/▶) and content. If headerOut is non-null and length > 0, stores the header button.
	 */
	public static JPanel createCollapsibleSection(String title, Component content, boolean expandedByDefault, JToggleButton[] headerOut)
	{
		JPanel wrapper = new JPanel(new java.awt.BorderLayout());
		wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
		JToggleButton header = new JToggleButton(expandedByDefault ? "▼ " + title : "▶ " + title, expandedByDefault);
		header.setFocusPainted(false);
		header.setBorderPainted(false);
		header.setContentAreaFilled(false);
		header.setHorizontalAlignment(SwingConstants.LEFT);
		content.setVisible(expandedByDefault);
		final String titleFinal = title;
		header.addActionListener(e -> {
			boolean on = header.isSelected();
			content.setVisible(on);
			header.setText(on ? "▼ " + titleFinal : "▶ " + titleFinal);
			wrapper.revalidate();
			for (Container p = wrapper.getParent(); p != null; p = p.getParent())
				p.revalidate();
			wrapper.repaint();
		});
		wrapper.add(header, java.awt.BorderLayout.NORTH);
		wrapper.add(content, java.awt.BorderLayout.CENTER);
		if (headerOut != null && headerOut.length > 0)
			headerOut[0] = header;
		return wrapper;
	}

	/**
	 * Shows a save dialog for JSON files with the given default filename. Ensures the selected file ends with .json.
	 * @return the selected file (with .json appended if needed), or null if cancelled
	 */
	public static File showJsonSaveDialog(Component parent, String defaultFileName)
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Save JSON");
		chooser.setSelectedFile(new File(defaultFileName != null ? defaultFileName : "export.json"));
		chooser.setFileFilter(new FileNameExtensionFilter("JSON files", "json"));
		if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION)
			return null;
		File file = chooser.getSelectedFile();
		if (file != null && !file.getName().toLowerCase().endsWith(".json"))
			file = new File(file.getParent(), file.getName() + ".json");
		return file;
	}

	/** Button with rectangle image background and pressed shadow. Use GridScapeColors.POPUP_TEXT for text color. */
	public static JButton newRectangleButton(String text, BufferedImage buttonRect, Color textColor)
	{
		BufferedImage img = buttonRect;
		JButton b = new JButton(text)
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				if (img != null)
				{
					ScaledImageCache.drawScaled(g, img, 0, 0, getWidth(), getHeight());
					g.setColor(getForeground());
					g.setFont(getFont());
					java.awt.FontMetrics fm = g.getFontMetrics();
					String t = getText();
					int x = (getWidth() - fm.stringWidth(t)) / 2;
					int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
					g.drawString(t, x, y);
				}
				else
					super.paintComponent(g);
				if (getModel().isPressed())
				{
					g.setColor(PRESSED_INSET_SHADOW);
					g.fillRect(PRESSED_INSET, PRESSED_INSET, getWidth() - 2 * PRESSED_INSET, getHeight() - 2 * PRESSED_INSET);
				}
			}
		};
		b.setForeground(textColor != null ? textColor : GridScapeColors.POPUP_TEXT);
		b.setFocusPainted(false);
		b.setBorderPainted(false);
		b.setContentAreaFilled(img == null);
		b.setOpaque(img == null);
		b.setPreferredSize(RECTANGLE_BUTTON_SIZE);
		return b;
	}

	/** Icon-only button (e.g. close) with pressed inset shadow. Use GridScapeColors.POPUP_TEXT for fallback text. */
	public static JButton newPopupButtonWithIcon(BufferedImage iconImg, Color fallbackTextColor)
	{
		JButton b = new JButton()
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				super.paintComponent(g);
				if (getModel().isPressed())
				{
					g.setColor(PRESSED_INSET_SHADOW);
					g.fillRect(PRESSED_INSET, PRESSED_INSET, getWidth() - 2 * PRESSED_INSET, getHeight() - 2 * PRESSED_INSET);
				}
			}
		};
		b.setFocusPainted(false);
		b.setBorderPainted(false);
		b.setContentAreaFilled(false);
		b.setMargin(new Insets(0, 0, 0, 0));
		if (iconImg != null)
			b.setIcon(new javax.swing.ImageIcon(ImageUtil.resizeImage(iconImg, 24, 24)));
		else
		{
			b.setText("X");
			b.setForeground(fallbackTextColor != null ? fallbackTextColor : GridScapeColors.POPUP_TEXT);
		}
		return b;
	}

	/**
	 * Makes an undecorated window draggable from {@code dragRegion} (e.g. title row), matching
	 * {@link com.gridscape.config.GridScapeSetupFrame} title-bar behaviour.
	 */
	public static void installUndecoratedWindowDrag(Window window, Component dragRegion)
	{
		if (window == null || dragRegion == null)
			return;
		final int[] dragOffset = new int[2];
		dragRegion.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				java.awt.Point loc = window.getLocationOnScreen();
				dragOffset[0] = e.getXOnScreen() - loc.x;
				dragOffset[1] = e.getYOnScreen() - loc.y;
			}
		});
		dragRegion.addMouseMotionListener(new MouseAdapter()
		{
			@Override
			public void mouseDragged(MouseEvent e)
			{
				window.setLocation(e.getXOnScreen() - dragOffset[0], e.getYOnScreen() - dragOffset[1]);
			}
		});
	}

	/** Registers Escape key to close the given window (dispose). Call after creating a JDialog/JFrame. */
	public static void registerEscapeToClose(java.awt.Window window)
	{
		if (window instanceof RootPaneContainer)
		{
			JRootPane rp = ((RootPaneContainer) window).getRootPane();
			rp.registerKeyboardAction(
				e -> window.dispose(),
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);
		}
	}

	public static void showTaskHubBookmarkMenu(MouseEvent e, GlobalTaskListService service, TaskTile tile,
		Runnable onButtonPress, Runnable onChanged)
	{
		if (!e.isPopupTrigger()) return;
		int r = tile.getRow(), c = tile.getCol();
		boolean bookmarked = service.isTaskHubBookmarked(r, c);
		JPopupMenu menu = new JPopupMenu();
		JMenuItem item = new JMenuItem(bookmarked ? "Remove bookmark" : "Add bookmark");
		item.addActionListener(ev -> {
			if (onButtonPress != null) onButtonPress.run();
			if (bookmarked)
				service.removeTaskHubBookmark(r, c);
			else
				service.addTaskHubBookmark(new GlobalTaskBookmark(
					GlobalTaskListService.taskKeyFromName(tile.getDisplayName()), r, c, ""));
			if (onChanged != null) onChanged.run();
		});
		menu.add(item);
		menu.show(e.getComponent(), e.getX(), e.getY());
	}

	public static java.awt.Frame resolveClientFrameOwner(Client client)
	{
		return TaskTileCellFactory.resolveDialogOwner(null, client);
	}

	/** Standard popup panel border/background used by grid panels and overlay dialogs. */
	public static void applyPopupPanelChrome(JPanel panel)
	{
		panel.setLayout(new BorderLayout(8, 8));
		panel.setBackground(GridScapeColors.POPUP_BG);
		panel.setBorder(new CompoundBorder(
			new LineBorder(GridScapeColors.POPUP_BORDER, 2),
			new EmptyBorder(10, 12, 10, 12)));
		panel.setOpaque(true);
	}

	/** Header row: optional points label (west), centered title, close button (east). */
	public static JPanel newGridPanelHeader(JLabel pointsLabel, String title, BufferedImage xBtnImg,
		Color textColor, Runnable onClose)
	{
		JPanel header = new JPanel(new BorderLayout(4, 0));
		header.setOpaque(false);
		header.setBorder(new EmptyBorder(0, 0, 8, 0));
		JPanel titleRow = new JPanel(new BorderLayout(4, 0));
		titleRow.setOpaque(false);
		if (pointsLabel != null)
		{
			pointsLabel.setForeground(textColor);
			titleRow.add(pointsLabel, BorderLayout.WEST);
		}
		JLabel titleLabel = new JLabel(title);
		titleLabel.setForeground(textColor);
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
		titleLabel.setHorizontalAlignment(JLabel.CENTER);
		titleRow.add(titleLabel, BorderLayout.CENTER);
		JButton closeBtn = newPopupButtonWithIcon(xBtnImg, textColor);
		closeBtn.addActionListener(e -> {
			if (onClose != null) onClose.run();
		});
		titleRow.add(closeBtn, BorderLayout.EAST);
		header.add(titleRow, BorderLayout.NORTH);
		return header;
	}

	/** Returns the title row inside {@link #newGridPanelHeader} for window drag installation. */
	public static JPanel titleRowFromHeader(JPanel header)
	{
		return (JPanel) header.getComponent(0);
	}

	public static void installGridScrollWheelZoom(JScrollPane scrollPane, float[] zoomHolder, float zoomMin,
		float zoomMax, float zoomStep, Runnable refresh)
	{
		scrollPane.getViewport().addMouseWheelListener(e -> {
			float prev = zoomHolder[0];
			if (e.getWheelRotation() < 0)
				zoomHolder[0] = Math.min(zoomMax, zoomHolder[0] + zoomStep);
			else
				zoomHolder[0] = Math.max(zoomMin, zoomHolder[0] - zoomStep);
			if (zoomHolder[0] != prev)
			{
				e.consume();
				SwingUtilities.invokeLater(refresh);
			}
		});
	}

	public static void installGridScrollDragPan(JScrollPane scrollPane, Point[] dragStart, boolean clearDragOnRelease)
	{
		scrollPane.getViewport().addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				dragStart[0] = e.getPoint();
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				if (clearDragOnRelease)
					dragStart[0] = null;
			}
		});
		scrollPane.getViewport().addMouseMotionListener(new MouseMotionAdapter()
		{
			@Override
			public void mouseDragged(MouseEvent e)
			{
				if (dragStart[0] == null) return;
				JViewport viewport = scrollPane.getViewport();
				Point vp = viewport.getViewPosition();
				int dx = dragStart[0].x - e.getX();
				int dy = dragStart[0].y - e.getY();
				int nx = Math.max(0, Math.min(vp.x + dx, viewport.getViewSize().width - viewport.getExtentSize().width));
				int ny = Math.max(0, Math.min(vp.y + dy, viewport.getViewSize().height - viewport.getExtentSize().height));
				viewport.setViewPosition(new Point(nx, ny));
				dragStart[0] = e.getPoint();
			}
		});
	}
}
