USE kingstore;

INSERT INTO user_account (active, email, password)
VALUES (
    1,
    'sgomezg@pucp.edu.pe',
    '$2a$10$AyplC0/UYUOZiUTjHHQoT.FITjeVhMVGsL9yNlVOY7lox615n6PC6'
);

SET @userAccountId = LAST_INSERT_ID();

INSERT INTO person (
    active,
    birth_date,
    document_number,
    document_type,
    first_name,
    gender,
    maternal_surname,
    paternal_surname,
    phone
)
VALUES (
    1,
    '2000-01-01',
    '12345678',
    'DNI',
    'Administrador',
    'MALE',
    'Sistema',
    'Principal',
    '999999999'
);

SET @personId = LAST_INSERT_ID();

INSERT INTO system_administrator (
    position,
    person_id,
    user_account_id
)
VALUES (
    'SUPER_ADMIN',
    @personId,
    @userAccountId
);