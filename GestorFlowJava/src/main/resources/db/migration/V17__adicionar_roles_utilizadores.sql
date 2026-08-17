-- 1. Adicionar a coluna de role com o valor por defeito 'USER'
ALTER TABLE utilizadores ADD COLUMN role VARCHAR(20) DEFAULT 'USER' NOT NULL;

-- 2. Dar-te poderes de Deus (Super Admin) através do teu email de registo!
UPDATE utilizadores
SET role = 'SUPER_ADMIN'
WHERE email = 'miguel10072001@icloud.com';