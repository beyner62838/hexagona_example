CREATE TABLE IF NOT EXISTS franchises (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS branches (
  id BIGSERIAL PRIMARY KEY,
  franchise_id BIGINT NOT NULL REFERENCES franchises(id) ON DELETE CASCADE,
  name VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS products (
  id BIGSERIAL PRIMARY KEY,
  branch_id BIGINT NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
  name VARCHAR(255) NOT NULL,
  stock INT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_branches_franchise_id ON branches(franchise_id);
CREATE INDEX IF NOT EXISTS idx_products_branch_id ON products(branch_id);
