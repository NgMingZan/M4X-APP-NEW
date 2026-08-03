-- M4X Theme v3.0.7 - M4X WEB online config
alter table public.app_config
  add column if not exists web_football_url text not null default 'https://xoilacxtl.tv/',
  add column if not exists web_movie_url text not null default 'https://cobephim.pro/',
  add column if not exists web_adult_url text not null default 'https://vnsextop1.com/',
  add column if not exists web_football_enabled boolean not null default true,
  add column if not exists web_movie_enabled boolean not null default true,
  add column if not exists web_adult_enabled boolean not null default true;

insert into public.app_config(id, web_football_url, web_movie_url, web_adult_url, web_football_enabled, web_movie_enabled, web_adult_enabled)
values('main','https://xoilacxtl.tv/','https://cobephim.pro/','https://vnsextop1.com/',true,true,true)
on conflict(id) do update set
  web_football_url=excluded.web_football_url,
  web_movie_url=excluded.web_movie_url,
  web_adult_url=excluded.web_adult_url;

notify pgrst,'reload schema';
