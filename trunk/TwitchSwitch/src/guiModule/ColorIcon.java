package guiModule;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.ImageIcon;

/**
 * This is a simple class which is used to create 14x14 icon images with a
 * specified color and a small black border.
 * 
 * 
 */
public class ColorIcon extends ImageIcon {

    /**
     * 
     */
    private static final long serialVersionUID = -6970235967215052177L;
    /**
     * Icon height.
     */
    private static final int HEIGHT = 14;
    /**
     * Icon width.
     */
    private static final int WIDTH = 14;

    /**
     * Icon color.
     */
    private final Color color;

    /**
     * Creates an icon with specified color.
     * 
     * @param color
     *            the color to paint the icon with.
     */
    public ColorIcon(final Color color) {
        this.color = color;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.Icon#getIconHeight()
     */
    @Override
    public int getIconHeight() {
        return HEIGHT;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.Icon#getIconWidth()
     */
    @Override
    public int getIconWidth() {
        return WIDTH;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.Icon#paintIcon(java.awt.Component, java.awt.Graphics,
     * int, int)
     */
    @Override
    public void paintIcon(final Component c, final Graphics g, final int x,
            final int y) {
        /* fill icon with color. */
        g.setColor(color);
        g.fillRect(x, y, WIDTH - 1, HEIGHT - 1);

        /* draw black border */
        g.setColor(Color.BLACK);
        g.drawRect(x, y, WIDTH - 1, HEIGHT - 1);
    }
}
