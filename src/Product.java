import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class Product {

    private String barcode;
    private String name;
    private double quantity;
    private String unit;
    private BigDecimal price;

    public Product(String barcode, String name, double quantity, String unit, BigDecimal price) {
        setBarcode(barcode);
        setName(name);
        setQuantity(quantity);
        setUnit(unit);
        setPrice(price);
    }

    public String getBarcode() { return barcode; }
    public String getName() { return name; }
    public double getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public BigDecimal getPrice() { return price; }

    public final void setBarcode(String barcode) {
        this.barcode = barcode == null ? null : barcode.trim();
    }

    public final void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public final void setUnit(String unit) {
        this.unit = unit == null ? "" : unit.trim();
    }

    public void setQuantity(double quantity) {
        if (Double.isNaN(quantity) || Double.isInfinite(quantity)) {
            throw new IllegalArgumentException("Quantity must be a valid number");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative!");
        }
        this.quantity = quantity;
    }

    public final void setPrice(BigDecimal price) {
        if (price == null) throw new IllegalArgumentException("Price cannot be null");
        if (price.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Price cannot be negative");
        this.price = price.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalValue() {
        return price.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(barcode, product.barcode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(barcode);
    }
}