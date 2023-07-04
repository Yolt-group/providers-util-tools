create table if not exists client_admin_invitation(
                                                            client_id uuid not null,
                                                            code varchar(256),
                                                            generated_at timestamp,
                                                            email varchar(256),
                                                            name varchar(256),
                                                            used timestamp,
                                                            PRIMARY KEY (client_id, code)
);

ALTER TABLE client_admin_invitation
    ADD CONSTRAINT fk_client_admin_code_id FOREIGN KEY (client_id) REFERENCES client (client_id);

create index if not exists ind_client_admin_invitation_code on client_admin_invitation(code, used);