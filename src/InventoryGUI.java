import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

public class InventoryGUI extends JFrame {

    private final JTextField txtBarcode = new JTextField(15);
    private final JTextField txtName = new JTextField(15);
    private final JTextField txtQuantity = new JTextField(8);
    private final JTextField txtPrice = new JTextField(8);
    private final JComboBox<String> comboUnit = new JComboBox<>(new String[]{"pcs","kg","g","liter","ml"});
    private final JComboBox<String> comboCurrency = new JComboBox<>(new String[]{"₹","$","€"});
    private final JTextField txtSearch = new JTextField(20);

    private final JTable table;
    private final DefaultTableModel model;
    private final TableRowSorter<DefaultTableModel> sorter;

    private final InventoryManager manager = InventoryManager.getInstance();
    private String originalBarcode = null; // track when updating

    private final JLabel lblTotal = new JLabel("Total: ₹0.00", SwingConstants.RIGHT);

    public InventoryGUI() {
        super("Inventory Management");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(new Dimension(1000, 600));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(root);

        // Form panel (left)
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Product Details"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0; form.add(new JLabel("Barcode:"), c);
        c.gridx = 1; form.add(txtBarcode, c);

        c.gridx = 0; c.gridy = 1; form.add(new JLabel("Name:"), c);
        c.gridx = 1; form.add(txtName, c);

        c.gridx = 0; c.gridy = 2; form.add(new JLabel("Quantity:"), c);
        c.gridx = 1; form.add(txtQuantity, c);

        c.gridx = 0; c.gridy = 3; form.add(new JLabel("Unit:"), c);
        c.gridx = 1; form.add(comboUnit, c);

        c.gridx = 0; c.gridy = 4; form.add(new JLabel("Price:"), c);
        c.gridx = 1; form.add(txtPrice, c);

        c.gridx = 0; c.gridy = 5; form.add(new JLabel("Currency:"), c);
        c.gridx = 1; form.add(comboCurrency, c);

        // Buttons
        JPanel actions = new JPanel();
        JButton btnAdd = new JButton("Add");
        JButton btnUpdate = new JButton("Update");
        JButton btnDelete = new JButton("Delete");
        JButton btnClear = new JButton("Clear");
        actions.add(btnAdd);
        actions.add(btnUpdate);
        actions.add(btnDelete);
        actions.add(btnClear);

        c.gridx = 0; c.gridy = 6; c.gridwidth = 2; form.add(actions, c);

        // Search bar at top-right
        JPanel topRight = new JPanel(new BorderLayout(6, 6));
        topRight.setBorder(BorderFactory.createTitledBorder("Search"));
        topRight.add(txtSearch, BorderLayout.CENTER);

        // Table
        model = new DefaultTableModel(new String[]{"Barcode","Name","Quantity","Unit","Price","Total"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model);

        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // Renderer to highlight low stock
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                try {
                    int modelRow = table.convertRowIndexToModel(row);
                    Object q = model.getValueAt(modelRow, 2);
                    double qty = Double.parseDouble(String.valueOf(q));
                    if (!isSelected) {
                        c.setBackground(qty < 5.0 ? new Color(255, 230, 230) : Color.WHITE);
                    }
                } catch (Exception e) {
                    // ignore parsing errors
                    if (!isSelected) c.setBackground(Color.WHITE);
                }
                return c;
            }
        });

        // Layout composition
        JPanel left = new JPanel(new BorderLayout());
        left.add(form, BorderLayout.NORTH);

        JPanel right = new JPanel(new BorderLayout(6,6));
        right.add(topRight, BorderLayout.NORTH);
        right.add(new JScrollPane(table), BorderLayout.CENTER);

        root.add(left, BorderLayout.WEST);
        root.add(right, BorderLayout.CENTER);

        // Bottom: total label
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(new EmptyBorder(6,6,6,6));
        bottom.add(lblTotal, BorderLayout.EAST);
        root.add(bottom, BorderLayout.SOUTH);

        // Load initial data
        loadAllProductsToTable();
        refreshTotalLabel();

        // Listeners
        btnAdd.addActionListener(e -> addProduct());
        btnUpdate.addActionListener(e -> updateProduct());
        btnDelete.addActionListener(e -> deleteProduct());
        btnClear.addActionListener(e -> clearForm());

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) populateFormFromSelection();
        });

        txtSearch.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override public void update() { applySearchFilter(); }
        });

        // Improve spacing
        setMinimumSize(new Dimension(900, 500));
    }

    private void loadAllProductsToTable() {
        model.setRowCount(0);
        List<Product> all = manager.getAllProducts();
        for (Product p : all) addRowForProduct(p);
    }

    private void addRowForProduct(Product p) {
        model.addRow(new Object[] {
            p.getBarcode(),
            p.getName(),
            String.valueOf(p.getQuantity()),
            p.getUnit(),
            p.getPrice().setScale(2, RoundingMode.HALF_UP).toString(),
            p.getTotalValue().setScale(2, RoundingMode.HALF_UP).toString()
        });
    }

    private void applySearchFilter() {
        String text = txtSearch.getText();
        if (text == null || text.trim().isEmpty()) {
            sorter.setRowFilter(null);
            return;
        }
        String q = text.trim();
        sorter.setRowFilter(javax.swing.RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(q)));
    }

    private void addProduct() {
        try {
            String barcode = txtBarcode.getText().trim();
            String name = txtName.getText().trim();
            String qtyText = txtQuantity.getText().trim();
            String priceText = txtPrice.getText().trim();
            String unit = String.valueOf(comboUnit.getSelectedItem());
            String currency = String.valueOf(comboCurrency.getSelectedItem());

            if (barcode.isEmpty() || name.isEmpty() || qtyText.isEmpty() || priceText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all required fields.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            double qty = Double.parseDouble(qtyText);
            if (qty < 0) throw new NumberFormatException("Negative quantity");
            BigDecimal price = new BigDecimal(priceText).setScale(2, RoundingMode.HALF_UP);
            if (price.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException("Negative price");

            if (manager.containsBarcode(barcode)) {
                JOptionPane.showMessageDialog(this, "Barcode already exists.", "Duplicate", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Product p = new Product(barcode, name, qty, unit, price);
            manager.addProduct(p);
            addRowForProduct(p);
            clearForm();
            refreshTotalLabel();
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Numeric fields invalid or negative.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException iae) {
            JOptionPane.showMessageDialog(this, iae.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Unexpected error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateProduct() {
        int sel = table.getSelectedRow();
        if (sel < 0) {
            JOptionPane.showMessageDialog(this, "Select a product to update.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            String barcode = txtBarcode.getText().trim();
            String name = txtName.getText().trim();
            String qtyText = txtQuantity.getText().trim();
            String priceText = txtPrice.getText().trim();
            String unit = String.valueOf(comboUnit.getSelectedItem());

            if (barcode.isEmpty() || name.isEmpty() || qtyText.isEmpty() || priceText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all required fields.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            double qty = Double.parseDouble(qtyText);
            if (qty < 0) throw new NumberFormatException("Negative quantity");
            BigDecimal price = new BigDecimal(priceText).setScale(2, RoundingMode.HALF_UP);
            if (price.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException("Negative price");

            Product updated = new Product(barcode, name, qty, unit, price);
            String old = originalBarcode != null ? originalBarcode : model.getValueAt(table.convertRowIndexToModel(sel), 0).toString();
            manager.updateProduct(old, updated);

            // update model row
            int modelRow = table.convertRowIndexToModel(sel);
            model.setValueAt(updated.getBarcode(), modelRow, 0);
            model.setValueAt(updated.getName(), modelRow, 1);
            model.setValueAt(String.valueOf(updated.getQuantity()), modelRow, 2);
            model.setValueAt(updated.getUnit(), modelRow, 3);
            model.setValueAt(updated.getPrice().toString(), modelRow, 4);
            model.setValueAt(updated.getTotalValue().toString(), modelRow, 5);

            clearForm();
            refreshTotalLabel();
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Numeric fields invalid or negative.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException iae) {
            JOptionPane.showMessageDialog(this, iae.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Unexpected error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            originalBarcode = null;
        }
    }

    private void deleteProduct() {
        int sel = table.getSelectedRow();
        if (sel < 0) {
            JOptionPane.showMessageDialog(this, "Select a product to delete.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete selected product?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        int modelRow = table.convertRowIndexToModel(sel);
        String barcode = model.getValueAt(modelRow, 0).toString();
        manager.deleteProduct(barcode);
        model.removeRow(modelRow);
        clearForm();
        refreshTotalLabel();
    }

    private void populateFormFromSelection() {
        int sel = table.getSelectedRow();
        if (sel < 0) return;
        int modelRow = table.convertRowIndexToModel(sel);
        originalBarcode = String.valueOf(model.getValueAt(modelRow, 0));
        txtBarcode.setText(originalBarcode);
        txtName.setText(String.valueOf(model.getValueAt(modelRow, 1)));
        txtQuantity.setText(String.valueOf(model.getValueAt(modelRow, 2)));
        comboUnit.setSelectedItem(String.valueOf(model.getValueAt(modelRow, 3)));
        txtPrice.setText(String.valueOf(model.getValueAt(modelRow, 4)));
    }

    private void clearForm() {
        txtBarcode.setText("");
        txtName.setText("");
        txtQuantity.setText("");
        txtPrice.setText("");
        comboUnit.setSelectedIndex(0);
        comboCurrency.setSelectedIndex(0);
        table.clearSelection();
        originalBarcode = null;
    }

    private void refreshTotalLabel() {
        try {
            BigDecimal total = manager.getTotalInventoryValue();
            lblTotal.setText("Total: " + comboCurrency.getSelectedItem() + " " + total.setScale(2, RoundingMode.HALF_UP).toString());
        } catch (Exception e) {
            lblTotal.setText("Total: -");
        }
    }

    // Minimal DocumentListener helper
    private interface SimpleDocumentListener extends javax.swing.event.DocumentListener {
        void update();
        @Override default void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
        @Override default void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
        @Override default void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
    }

    // Show UI on EDT
    public static void showGui() {
        SwingUtilities.invokeLater(() -> {
            InventoryGUI gui = new InventoryGUI();
            gui.setVisible(true);
        });
    }
}