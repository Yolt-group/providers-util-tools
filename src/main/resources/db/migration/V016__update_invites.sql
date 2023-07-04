CREATE TABLE IF NOT EXISTS client_admin_invitation_v2
(
    id        uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    client_id uuid         not null,
    email     varchar(256) not null,
    name      varchar(256) not null
);

CREATE TABLE IF NOT EXISTS client_admin_invitation_codes
(
    id           uuid      not null,
    code         varchar(256) PRIMARY KEY,
    generated_at timestamp not null,
    expires_at   timestamp not null,
    used_at      timestamp,
    used_by      uuid
);

ALTER TABLE client_admin_invitation_v2
    ADD CONSTRAINT unique_client_admin_invitation_v2_client_email UNIQUE (client_id, email, name);
ALTER TABLE client_admin_invitation_codes
    ADD CONSTRAINT fk_client_admin_invitation_v2_id FOREIGN KEY (id) REFERENCES client_admin_invitation_v2 (id);

/* importing is done through batch trigger
INSERT INTO client_admin_invitation_v2 (client_id, email, name)
Select distinct on (T1.client_id, T1.email, T1.name) T1.client_id, T1.email, T1.name
FROM client_admin_invitation T1
ORDER BY T1.client_id, T1.email, T1.name;

INSERT INTO client_admin_invitation_codes (id, code, generated_at, expires_at, used_at)
SELECT T2.id, T1.code, T1.generated_at, T1.generated_at + INTERVAL '1 day', T1.used
FROM client_admin_invitation T1
         JOIN client_admin_invitation_v2 T2
              ON T1.client_id = T2.client_id AND T1.email = T2.email AND T1.name = T2.name;
 */

CREATE TABLE IF NOT EXISTS client_group_admin_invitation_v2
(
    id              uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    client_group_id uuid         not null,
    email           varchar(256) not null,
    name            varchar(256) not null
);

CREATE TABLE IF NOT EXISTS client_group_admin_invitation_codes
(
    id           uuid      not null,
    code         varchar(256) PRIMARY KEY,
    generated_at timestamp not null,
    expires_at   timestamp not null,
    used_at      timestamp,
    used_by      uuid
);

ALTER TABLE client_group_admin_invitation_v2
    ADD CONSTRAINT unique_client_group_admin_invitation_v2_client_email UNIQUE (client_group_id, email, name);
ALTER TABLE client_group_admin_invitation_codes
    ADD CONSTRAINT fk_client_group_admin_invitation_v2_id FOREIGN KEY (id) REFERENCES client_group_admin_invitation_v2 (id);
/* importing is done through batch trigger
INSERT INTO client_group_admin_invitation_v2 (client_group_id, email, name)
Select distinct on (T1.client_group_id, T1.email, T1.name) T1.client_group_id, T1.email, T1.name
FROM client_group_admin_invitation T1
ORDER BY T1.client_group_id, T1.email, T1.name;

INSERT INTO client_group_admin_invitation_codes (id, code, generated_at, expires_at, used_at)
SELECT T2.id, T1.code, T1.generated_at, T1.generated_at + INTERVAL '1 day', T1.used
FROM client_group_admin_invitation T1
         JOIN client_group_admin_invitation_v2 T2
              ON T1.client_group_id = T2.client_group_id AND T1.email = T2.email AND T1.name = T2.name;
 */
