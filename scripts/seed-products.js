// Seeds product_service.products with 1000 products, sku_code SKU-00001..SKU-01000,
// matching the SKUs seed-inventory.sql inserts into inventory_service.inventory.
// Run: docker exec -i mongodb mongosh product_service < scripts/seed-products.js

const categories = [
  { name: "Teclado", desc: "Teclado mecanico" },
  { name: "Mouse", desc: "Mouse otico" },
  { name: "Monitor", desc: "Monitor LED" },
  { name: "Headset", desc: "Fone com microfone" },
  { name: "Webcam", desc: "Camera USB" },
  { name: "SSD", desc: "Unidade de estado solido" },
  { name: "Cadeira", desc: "Cadeira ergonomica" },
  { name: "Mousepad", desc: "Base para mouse" },
  { name: "Hub USB", desc: "Adaptador USB de multiplas portas" },
  { name: "Carregador", desc: "Fonte de alimentacao" },
];

db.products.deleteMany({ sku_code: { $regex: /^SKU-\d{5}$/ } });

const now = new Date();
const batchSize = 500;
let batch = [];

for (let i = 1; i <= 1000; i++) {
  const sku = "SKU-" + String(i).padStart(5, "0");
  const category = categories[i % categories.length];
  const price = NumberDecimal(((i % 97) * 3.37 + 9.9).toFixed(2));

  batch.push({
    sku_code: sku,
    name: category.name + " " + i,
    description: category.desc + " - modelo " + i,
    price: price,
    created_at: now,
    updated_at: now,
  });

  if (batch.length === batchSize) {
    db.products.insertMany(batch);
    batch = [];
  }
}
if (batch.length > 0) {
  db.products.insertMany(batch);
}

print("Seeded " + db.products.countDocuments({ sku_code: { $regex: /^SKU-\d{5}$/ } }) + " products");
