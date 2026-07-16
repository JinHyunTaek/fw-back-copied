create table promotion
(
    promotion_id            bigint       not null auto_increment,
    title                   varchar(255),
    benefit                 varchar(255),
    notice                  varchar(1000),
    start_date              date,
    end_date                date,
    announce_date           date,
    max_winner_count        integer      not null,
    drawn_at                datetime(6),
    created_date_time       datetime(6),
    last_modified_date_time datetime(6),
    primary key (promotion_id)
) engine = InnoDB
  default charset = utf8mb4;

create table gifticon
(
    gifticon_id             bigint       not null auto_increment,
    promotion_id            bigint,
    name                    varchar(255),
    coupon_number           varchar(255),
    category                enum ('CHICKEN','COFFEE','CONVENIENCE','DELIVERY','DESSERT'),
    image_key               varchar(255),
    expiry_date             date,
    display_order           integer      not null,
    is_assigned             bit          not null,
    created_date_time       datetime(6),
    last_modified_date_time datetime(6),
    primary key (gifticon_id),
    constraint uk_gifticon_coupon_number unique (coupon_number),
    constraint fk_gifticon_promotion foreign key (promotion_id) references promotion (promotion_id)
) engine = InnoDB
  default charset = utf8mb4;

create table promotion_winner
(
    promotion_winner_id     bigint not null auto_increment,
    promotion_id            bigint,
    user_id                 bigint,
    gifticon_id             bigint,
    send_status             enum ('FAILED','PENDING','SENT'),
    created_date_time       datetime(6),
    last_modified_date_time datetime(6),
    primary key (promotion_winner_id),
    constraint uk_winner_gifticon unique (gifticon_id),
    constraint uk_winner_promotion_user unique (promotion_id, user_id),
    constraint fk_winner_promotion foreign key (promotion_id) references promotion (promotion_id),
    constraint fk_winner_gifticon foreign key (gifticon_id) references gifticon (gifticon_id),
    index idx_winner_user (user_id)
) engine = InnoDB
  default charset = utf8mb4;
