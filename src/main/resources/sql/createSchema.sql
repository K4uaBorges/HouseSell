create table if not exists users (
                       uid uuid primary key,
                       name varchar(100) not null,
                       email text not null unique,
                       password_hash char(128) not null,
                       token uuid not null unique,
                       role text not null default 'USER',
                       constraint chk_user_role
                           check (role in ('ADMIN', 'USER'))
);

create table if not exists locations (
                           lid uuid primary key,
                           name text not null unique,
                           loc_type text not null,
                           parent_lid uuid,
                           constraint fk_location_parent
                               foreign key (parent_lid) references locations(lid) on delete set null,
                           constraint chk_location_type
                               check (loc_type in ('COUNTRY', 'REGION', 'DISTRICT', 'MUNICIPALITY', 'LOCALITY')),
                           constraint loc_parent_or_country
                               check (
                                   (loc_type = 'COUNTRY' and parent_lid is null) or
                                   (loc_type != 'COUNTRY' and parent_lid is not null)
                                   )
);

create table if not exists houses (
                        hid uuid primary key,
                        uid uuid not null,
                        title text not null,
                        location uuid not null,
                        areaSqMt int not null,
                        pricePerNight numeric not null,
                        description text not null,

                        constraint fk_house_owner
                            foreign key (uid) references users(uid) on delete cascade,
                        constraint fk_house_location
                            foreign key (location) references locations(lid) on delete cascade,
                        constraint chk_area
                            check (areasqmt > 0),
                        constraint chk_price
                            check (pricepernight > 0)
);

create table if not exists booking (
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

create index if not exists idx_booking_hid on booking(hid);
create index if not exists idx_booking_dates on booking(start_date, end_date);
create index if not exists idx_booking_user on booking(uid);
create index if not exists idx_houses_location on houses(location);
create index if not exists idx_houses_owner on houses(uid);
create index if not exists idx_locations_parent on locations(parent_lid);
