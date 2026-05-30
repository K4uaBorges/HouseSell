
insert into locations (lid, name, loc_type, parent_lid)
values ('11111111-1111-1111-1111-111111111111', 'Portugal', 'COUNTRY', null)
on conflict (name) do nothing;

insert into locations (lid, name, loc_type, parent_lid)
values
    ('11111111-1111-1111-1111-111111111112', 'Norte', 'REGION', (select lid from locations where name = 'Portugal')),
    ('11111111-1111-1111-1111-111111111113', 'Centro', 'REGION', (select lid from locations where name = 'Portugal')),
    ('11111111-1111-1111-1111-111111111114', 'Lisboa e Vale do Tejo', 'REGION', (select lid from locations where name = 'Portugal')),
    ('11111111-1111-1111-1111-111111111115', 'Alentejo', 'REGION', (select lid from locations where name = 'Portugal')),
    ('11111111-1111-1111-1111-111111111116', 'Algarve', 'REGION', (select lid from locations where name = 'Portugal')),
    ('11111111-1111-1111-1111-111111111117', 'Acores', 'REGION', (select lid from locations where name = 'Portugal')),
    ('11111111-1111-1111-1111-111111111118', 'Madeira', 'REGION', (select lid from locations where name = 'Portugal'))
on conflict (name) do nothing;

insert into locations (lid, name, loc_type, parent_lid)
values
    ('11111111-1111-1111-1111-111111111119', 'Porto (Distrito)', 'DISTRICT', (select lid from locations where name = 'Norte')),
    ('11111111-1111-1111-1111-111111111120', 'Braga (Distrito)', 'DISTRICT', (select lid from locations where name = 'Norte')),
    ('11111111-1111-1111-1111-111111111121', 'Aveiro (Distrito)', 'DISTRICT', (select lid from locations where name = 'Centro')),
    ('11111111-1111-1111-1111-111111111122', 'Coimbra (Distrito)', 'DISTRICT', (select lid from locations where name = 'Centro')),
    ('11111111-1111-1111-1111-111111111123', 'Lisboa (Distrito)', 'DISTRICT', (select lid from locations where name = 'Lisboa e Vale do Tejo')),
    ('11111111-1111-1111-1111-111111111124', 'Setubal (Distrito)', 'DISTRICT', (select lid from locations where name = 'Lisboa e Vale do Tejo')),
    ('11111111-1111-1111-1111-111111111125', 'Evora (Distrito)', 'DISTRICT', (select lid from locations where name = 'Alentejo')),
    ('11111111-1111-1111-1111-111111111126', 'Faro (Distrito)', 'DISTRICT', (select lid from locations where name = 'Algarve')),
    ('11111111-1111-1111-1111-111111111127', 'Ponta Delgada (Distrito)', 'DISTRICT', (select lid from locations where name = 'Acores')),
    ('11111111-1111-1111-1111-111111111128', 'Funchal (Distrito)', 'DISTRICT', (select lid from locations where name = 'Madeira'))
on conflict (name) do nothing;

insert into locations (lid, name, loc_type, parent_lid)
values
    ('11111111-1111-1111-1111-111111111129', 'Porto (Municipio)', 'MUNICIPALITY', (select lid from locations where name = 'Porto (Distrito)')),
    ('11111111-1111-1111-1111-111111111130', 'Vila Nova de Gaia (Municipio)', 'MUNICIPALITY', (select lid from locations where name = 'Porto (Distrito)')),
    ('11111111-1111-1111-1111-111111111131', 'Braga (Municipio)', 'MUNICIPALITY', (select lid from locations where name = 'Braga (Distrito)')),
    ('11111111-1111-1111-1111-111111111132', 'Guimaraes (Municipio)', 'MUNICIPALITY', (select lid from locations where name = 'Braga (Distrito)')),
    ('11111111-1111-1111-1111-111111111133', 'Aveiro (Municipio)', 'MUNICIPALITY', (select lid from locations where name = 'Aveiro (Distrito)')),
    ('11111111-1111-1111-1111-111111111134', 'Coimbra (Municipio)', 'MUNICIPALITY', (select lid from locations where name = 'Coimbra (Distrito)')),
    ('11111111-1111-1111-1111-111111111135', 'Lisboa (Municipio)', 'MUNICIPALITY', (select lid from locations where name = 'Lisboa (Distrito)')),
    ('11111111-1111-1111-1111-111111111136', 'Sintra', 'MUNICIPALITY', (select lid from locations where name = 'Lisboa (Distrito)')),
    ('11111111-1111-1111-1111-111111111137', 'Setubal (Municipio)', 'MUNICIPALITY', (select lid from locations where name = 'Setubal (Distrito)')),
    ('11111111-1111-1111-1111-111111111138', 'Evora (Municipio)', 'MUNICIPALITY', (select lid from locations where name = 'Evora (Distrito)')),
    ('11111111-1111-1111-1111-111111111139', 'Faro (Municipio)', 'MUNICIPALITY', (select lid from locations where name = 'Faro (Distrito)')),
    ('11111111-1111-1111-1111-111111111140', 'Loule', 'MUNICIPALITY', (select lid from locations where name = 'Faro (Distrito)')),
    ('11111111-1111-1111-1111-111111111141', 'Ponta Delgada (Municipio)', 'MUNICIPALITY', (select lid from locations where name = 'Ponta Delgada (Distrito)')),
    ('11111111-1111-1111-1111-111111111142', 'Funchal (Municipio)', 'MUNICIPALITY', (select lid from locations where name = 'Funchal (Distrito)'))
on conflict (name) do nothing;

insert into locations (lid, name, loc_type, parent_lid)
values
    ('11111111-1111-1111-1111-111111111143', 'Bonfim', 'LOCALITY', (select lid from locations where name = 'Porto (Municipio)')),
    ('11111111-1111-1111-1111-111111111144', 'Cedofeita', 'LOCALITY', (select lid from locations where name = 'Porto (Municipio)')),
    ('11111111-1111-1111-1111-111111111145', 'Canidelo', 'LOCALITY', (select lid from locations where name = 'Vila Nova de Gaia (Municipio)')),
    ('11111111-1111-1111-1111-111111111146', 'Mafamude', 'LOCALITY', (select lid from locations where name = 'Vila Nova de Gaia (Municipio)')),
    ('11111111-1111-1111-1111-111111111147', 'Sao Victor', 'LOCALITY', (select lid from locations where name = 'Braga (Municipio)')),
    ('11111111-1111-1111-1111-111111111148', 'Urgezes', 'LOCALITY', (select lid from locations where name = 'Guimaraes (Municipio)')),
    ('11111111-1111-1111-1111-111111111149', 'Gloria', 'LOCALITY', (select lid from locations where name = 'Aveiro (Municipio)')),
    ('11111111-1111-1111-1111-111111111150', 'Santo Antonio dos Olivais', 'LOCALITY', (select lid from locations where name = 'Coimbra (Municipio)')),
    ('11111111-1111-1111-1111-111111111151', 'Alvalade', 'LOCALITY', (select lid from locations where name = 'Lisboa (Municipio)')),
    ('11111111-1111-1111-1111-111111111152', 'Belem', 'LOCALITY', (select lid from locations where name = 'Lisboa (Municipio)')),
    ('11111111-1111-1111-1111-111111111153', 'Queluz', 'LOCALITY', (select lid from locations where name = 'Sintra')),
    ('11111111-1111-1111-1111-111111111154', 'Massama', 'LOCALITY', (select lid from locations where name = 'Sintra')),
    ('11111111-1111-1111-1111-111111111155', 'Azeitao', 'LOCALITY', (select lid from locations where name = 'Setubal (Municipio)')),
    ('11111111-1111-1111-1111-111111111156', 'Evora Historic Center', 'LOCALITY', (select lid from locations where name = 'Evora (Municipio)')),
    ('11111111-1111-1111-1111-111111111157', 'Montenegro', 'LOCALITY', (select lid from locations where name = 'Faro (Municipio)')),
    ('11111111-1111-1111-1111-111111111158', 'Quarteira', 'LOCALITY', (select lid from locations where name = 'Loule')),
    ('11111111-1111-1111-1111-111111111159', 'Sao Pedro', 'LOCALITY', (select lid from locations where name = 'Ponta Delgada (Municipio)'))
on conflict (name) do nothing;
