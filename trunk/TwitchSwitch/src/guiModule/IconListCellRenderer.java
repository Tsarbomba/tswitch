package guiModule;

import java.awt.Color;
import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 * A ListCellRenderer that allows for each cell in a JList to also display
 * either a green or gray icon. This is a highly specialized class which expects
 * each JList data value to be an Object[] where the first element is the
 * displayed text and the second element a boolean value where True = Gray icon,
 * False = Green icon.
 * 
 * 
 */
class IconListCellRenderer extends JLabel implements ListCellRenderer<Object> {

    private static final long serialVersionUID = 1L;

    private final ImageIcon greenIcon = new ColorIcon(Color.GREEN);
    private final ImageIcon grayIcon = new ColorIcon(Color.GRAY);

    @Override
    public Component getListCellRendererComponent(final JList<?> list,
            final Object value, final int index, final boolean isSelected,
            final boolean cellHasFocus) {
        try {
            final Object[] data = ((Object[]) value);
            if (data != null) {
                setText((String) data[0]);
                if ((Boolean) data[1]) {
                    setIcon(greenIcon);
                } else {
                    setIcon(grayIcon);
                }
            }
        } catch (final ClassCastException e) {
            /*
             * This should never happen unless the programmer has no idea
             * how/why he's using this class.
             */
            throw new ClassCastException("CellRenderer failed to cast Object[]");
        }
        return this;

    }
}
