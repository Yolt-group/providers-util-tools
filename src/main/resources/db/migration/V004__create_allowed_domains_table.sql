create table if not exists client_group_allowed_email_domain(
                                           client_group_id uuid not null,
                                           domain varchar(256) not null,
                                           PRIMARY KEY (client_group_id, domain)
);

create table if not exists client_group_admin_invitation(
                client_group_id uuid not null,
                code varchar(256),
                generated_at timestamp,
                email varchar(256),
                name varchar(256),
                used timestamp,
                PRIMARY KEY (client_group_id, code)
);

ALTER TABLE client_group_allowed_email_domain
    ADD CONSTRAINT fk_client_group_allowed_email_domain_id FOREIGN KEY (client_group_id) REFERENCES client_group (id);

ALTER TABLE client_group_admin_invitation
    ADD CONSTRAINT fk_client_group_admin_code_id FOREIGN KEY (client_group_id) REFERENCES client_group (id);