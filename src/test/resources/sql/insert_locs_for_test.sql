with countries as (
    insert into locations (lid, name, loc_type, parent_lid) values
                                                                (gen_random_uuid(), 'CountryA', 'COUNTRY', null),
                                                                (gen_random_uuid(), 'CountryB', 'COUNTRY', null),
                                                                (gen_random_uuid(), 'CountryC', 'COUNTRY', null)
        RETURNING lid, name
),
     region as (
         INSERT INTO locations (lid, name, loc_type, parent_lid)
             SELECT gen_random_uuid(), 'RegA', 'REGION', lid FROM countries WHERE name = 'CountryA'
             UNION ALL
             SELECT gen_random_uuid(), 'RegB', 'REGION', lid FROM countries WHERE name = 'CountryA'
             UNION ALL
             SELECT gen_random_uuid(), 'RegC', 'REGION', lid FROM countries WHERE name = 'CountryB'
             RETURNING lid, name
     ),
     district AS (
         INSERT INTO locations (lid, name, loc_type, parent_lid)
             SELECT gen_random_uuid(), 'DistA', 'DISTRICT', lid FROM region WHERE name = 'RegA'
             UNION ALL
             SELECT gen_random_uuid(), 'DistB', 'DISTRICT', lid FROM region WHERE name = 'RegC'
             UNION ALL
             SELECT gen_random_uuid(), 'DistC', 'DISTRICT', lid FROM region WHERE name = 'RegC'
             RETURNING lid, name
     ),
     municipality AS (
         INSERT INTO locations (lid, name, loc_type, parent_lid)
             SELECT gen_random_uuid(), 'MunA', 'MUNICIPALITY', lid FROM district WHERE name = 'DistB'
             UNION ALL
             SELECT gen_random_uuid(), 'MunB', 'MUNICIPALITY', lid FROM district WHERE name = 'DistB'
             UNION ALL
             SELECT gen_random_uuid(), 'MunC', 'MUNICIPALITY', lid FROM district WHERE name = 'DistC'
             RETURNING lid, name
     )
INSERT INTO locations (lid, name, loc_type, parent_lid)
SELECT gen_random_uuid(), 'LocA', 'LOCALITY', lid FROM municipality WHERE name = 'MunC'
UNION ALL
SELECT gen_random_uuid(), 'LocB', 'LOCALITY', lid FROM municipality WHERE name = 'MunC'
UNION ALL
SELECT gen_random_uuid(), 'LocC', 'LOCALITY', lid FROM municipality WHERE name = 'MunA';