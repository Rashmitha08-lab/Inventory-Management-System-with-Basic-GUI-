import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InventoryManager {

    private static volatile InventoryManager instance;
    private final Map<String, Product> products;

    private InventoryManager() {
        products = new ConcurrentHashMap<>();
    }

    public static InventoryManager getInstance() {
        if (instance == null) {
            synchronized (InventoryManager.class) {
                if (instance == null) {
                    instance = new InventoryManager();
                }
            }
        }
        return instance;
    }

    public void addProduct(Product product) {
        if (product == null) throw new IllegalArgumentException("Product cannot be null");
        String code = product.getBarcode();
        if (code == null || code.isEmpty()) throw new IllegalArgumentException("Product barcode required");
        if (products.putIfAbsent(code, product) != null) {
            throw new IllegalArgumentException("Product with barcode already exists: " + code);
        }
    }

    public void updateProduct(String oldBarcode, Product updated) {
        if (oldBarcode == null || oldBarcode.isEmpty()) throw new IllegalArgumentException("Old barcode required");
        if (updated == null) throw new IllegalArgumentException("Updated product required");
        String newBarcode = updated.getBarcode();
        if (newBarcode == null || newBarcode.isEmpty()) throw new IllegalArgumentException("Product barcode required");

        synchronized (products) {
            if (!products.containsKey(oldBarcode)) {
                throw new IllegalArgumentException("No product with barcode: " + oldBarcode);
            }
            if (!oldBarcode.equals(newBarcode) && products.containsKey(newBarcode)) {
                throw new IllegalArgumentException("Product with barcode already exists: " + newBarcode);
            }
            products.remove(oldBarcode);
            products.put(newBarcode, updated);
        }
    }

    public void deleteProduct(String barcode) {
        if (barcode == null) return;
        products.remove(barcode);
    }

    public Product getProductByBarcode(String barcode) {
        if (barcode == null) return null;
        return products.get(barcode);
    }

    public boolean containsBarcode(String barcode) {
        return barcode != null && products.containsKey(barcode);
    }

    public List<Product> getAllProducts() {
        return new ArrayList<>(products.values());
    }

    public List<Product> search(String query) {
        List<Product> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            results.addAll(getAllProducts());
            return results;
        }
        String q = query.trim().toLowerCase();
        for (Product p : products.values()) {
            if ((p.getBarcode() != null && p.getBarcode().toLowerCase().contains(q)) ||
                (p.getName() != null && p.getName().toLowerCase().contains(q))) {
                results.add(p);
            }
        }
        return results;
    }

    public BigDecimal getTotalInventoryValue() {
        BigDecimal total = BigDecimal.ZERO;
        for (Product p : products.values()) {
            try {
                total = total.add(p.getTotalValue());
            } catch (Exception e) {
                // ignore malformed product values
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}