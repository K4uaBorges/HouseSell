drop table if exists booking;
drop table if exists houses;
drop table if exists location;
drop table if exists users;


create table users (
                       uid uuid primary key,
                       name varchar(100) not null,
                       email text not null unique,
                       token uuid not null unique
);

create table location (
                          lid uuid primary key,
                          name text not null,
                          type varchar(13) not null,
                          parentid uuid,

                          constraint fk_location_parent
                              foreign key (parentid) references location(lid) on delete set null,

                          constraint chk_location
                              check (type in ('COUNTRY', 'REGION', 'DISTRICT', 'MUNICIPALITY', 'LOCALITY'))
);



create table houses (
                        hid uuid primary key,
                        uid uuid not null,
                        title text not null,
                        lid uuid not null,
                        areasqmt int not null,
                        pricepernight numeric not null,
                        description text not null,

                        constraint fk_house_owner
                            foreign key (uid) references users(uid) on delete cascade,
                        constraint fk_house_location
                            foreign key (lid) references location(lid),

                        constraint chk_area
                            check (areasqmt > 0),
                        constraint chk_price
                            check (pricepernight > 0)
);

create table booking (
                         id uuid primary key,
                         hid uuid not null,
                         uid uuid not null,
                         start_date date not null,
                         end_date date not null,

                         constraint fk_bookings_house
                             foreign key (hid) references houses(hid) on delete cascade,
                         constraint fk_bookings_user
                             foreign key (uid) references users(uid) on delete cascade,
                         constraint chk_booking_dates
                             check (start_date < end_date)
);
