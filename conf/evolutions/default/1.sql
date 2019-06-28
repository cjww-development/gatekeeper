# Registered Applications

# --- !Ups
CREATE TABLE if not exists registered_applications(
    id bigint(20) NOT NULL AUTO_INCREMENT,
    name varchar(50) NOT NULL UNIQUE,
    description text NOT NULL,
    home_url varchar(100) NOT NULL,
    redirect_url varchar(100) NOT NULL,
    client_type varchar(12) NOT NULL,
    client_id char(64) NOT NULL UNIQUE,
    client_secret char(107) UNIQUE,
    PRIMARY KEY (id)
);

# --- !Downs
DROP TABLE registered_applications;