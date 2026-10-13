-- V1: schema gốc của TripNova (pg_dump --schema-only ngày 2026-10-13, đã gồm mọi migration cũ trong db/legacy-migrations).
-- DB đang chạy sẵn KHÔNG chạy file này: Flyway baseline ở version 1 (spring.flyway.baseline-on-migrate) rồi chỉ chạy V2+.
-- Chỉ chạy trên DB trống (VD docker compose). Không chứa dữ liệu.
-- Thay đổi schema về sau: tạo file mới V2__mo_ta.sql, V3__... — KHÔNG sửa file đã chạy.

-- Hàm SQL tham chiếu bảng tạo sau -> tắt kiểm tra thân hàm trong transaction này.
SET LOCAL check_function_bodies = false;

-- Role cho dịch vụ AI (mật khẩu đặt ngoài migration: docker/db-init hoặc ALTER ROLE ... PASSWORD bằng tay).
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'chatbot_ro') THEN
        CREATE ROLE chatbot_ro LOGIN;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'chatbot_rag') THEN
        CREATE ROLE chatbot_rag LOGIN;
    END IF;
END $$;
ALTER ROLE chatbot_ro SET default_transaction_read_only = on;
ALTER ROLE chatbot_ro SET statement_timeout = '3s';
ALTER ROLE chatbot_ro SET search_path = chatbot;
ALTER ROLE chatbot_ro SET timezone = 'Asia/Ho_Chi_Minh';
ALTER ROLE chatbot_ro CONNECTION LIMIT 10;
ALTER ROLE chatbot_rag SET statement_timeout = '30s';
ALTER ROLE chatbot_rag CONNECTION LIMIT 10;

--
-- PostgreSQL database dump
--

-- Dumped from database version 16.14
-- Dumped by pg_dump version 16.14

--
-- Name: chatbot; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA chatbot;

--
-- Name: pg_trgm; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;

--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;

--
-- Name: unaccent; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS unaccent WITH SCHEMA public;

--
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;

--
-- Name: availability_status; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.availability_status AS ENUM (
    'available',
    'booked',
    'blocked'
);

--
-- Name: booking_status; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.booking_status AS ENUM (
    'pending',
    'confirmed',
    'checked_in',
    'checked_out',
    'cancelled',
    'refunded',
    'completed',
    'no_show'
);

--
-- Name: car_type; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.car_type AS ENUM (
    'sedan',
    'suv',
    'minivan',
    'luxury',
    'motorbike',
    'coach'
);

--
-- Name: cart_item_type; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.cart_item_type AS ENUM (
    'hotel',
    'tour',
    'car',
    'flight'
);

--
-- Name: contact_status; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.contact_status AS ENUM (
    'new',
    'in_progress',
    'resolved',
    'spam',
    'closed'
);

--
-- Name: flight_seat_class; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.flight_seat_class AS ENUM (
    'economy',
    'premium_economy',
    'business',
    'first'
);

--
-- Name: flight_seat_status; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.flight_seat_status AS ENUM (
    'available',
    'held',
    'booked',
    'blocked'
);

--
-- Name: landmark_category; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.landmark_category AS ENUM (
    'museum',
    'beach',
    'temple',
    'park',
    'mountain',
    'market',
    'restaurant',
    'entertainment',
    'historical',
    'other',
    'wellness',
    'sport'
);

--
-- Name: oauth_provider; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.oauth_provider AS ENUM (
    'google',
    'facebook',
    'apple',
    'zalo',
    'github'
);

--
-- Name: order_status; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.order_status AS ENUM (
    'pending',
    'paid',
    'partially_refunded',
    'refunded',
    'cancelled'
);

--
-- Name: payment_method; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.payment_method AS ENUM (
    'credit_card',
    'debit_card',
    'bank_transfer',
    'e_wallet',
    'momo',
    'zalopay',
    'vnpay',
    'cash'
);

--
-- Name: payment_status; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.payment_status AS ENUM (
    'pending',
    'success',
    'failed',
    'refunded'
);

--
-- Name: post_status; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.post_status AS ENUM (
    'pending',
    'approved',
    'rejected'
);

--
-- Name: reaction_type; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.reaction_type AS ENUM (
    'like',
    'dislike'
);

--
-- Name: room_status; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.room_status AS ENUM (
    'available',
    'occupied',
    'maintenance'
);

--
-- Name: user_gender; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.user_gender AS ENUM (
    'male',
    'female',
    'other'
);

--
-- Name: norm(text); Type: FUNCTION; Schema: chatbot; Owner: -
--

CREATE FUNCTION chatbot.norm(t text) RETURNS text
    LANGUAGE sql IMMUTABLE STRICT PARALLEL SAFE
    AS $_$ SELECT lower(public.unaccent('public.unaccent'::regdictionary, translate($1, 'đĐ', 'dD'))) $_$;

--
-- Name: is_room_available(uuid, date, date); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.is_room_available(p_room_id uuid, p_check_in date, p_check_out date) RETURNS boolean
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_blocked INT;
BEGIN
    -- Đếm ngày bị blocked hoặc đã booked trong khoảng [check_in, check_out)
    SELECT COUNT(*) INTO v_blocked
    FROM room_availability
    WHERE room_id = p_room_id
      AND date >= p_check_in
      AND date <  p_check_out
      AND status != 'available';

    RETURN v_blocked = 0;
END;
$$;

--
-- Name: set_updated_at(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.set_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

--
-- Name: sync_hotel_total_rooms(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.sync_hotel_total_rooms() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_old_hotel_id uuid;
    v_new_hotel_id uuid;
BEGIN
    IF TG_OP IN ('DELETE', 'UPDATE') THEN
        SELECT hotel_id INTO v_old_hotel_id FROM room_types WHERE id = OLD.room_type_id;
    END IF;
    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        SELECT hotel_id INTO v_new_hotel_id FROM room_types WHERE id = NEW.room_type_id;
    END IF;

    IF v_old_hotel_id IS NOT NULL THEN
        UPDATE hotels SET total_rooms = (
            SELECT count(*) FROM rooms r
            JOIN room_types rt ON rt.id = r.room_type_id
            WHERE rt.hotel_id = v_old_hotel_id
        ) WHERE id = v_old_hotel_id;
    END IF;

    IF v_new_hotel_id IS NOT NULL AND v_new_hotel_id IS DISTINCT FROM v_old_hotel_id THEN
        UPDATE hotels SET total_rooms = (
            SELECT count(*) FROM rooms r
            JOIN room_types rt ON rt.id = r.room_type_id
            WHERE rt.hotel_id = v_new_hotel_id
        ) WHERE id = v_new_hotel_id;
    END IF;

    RETURN NULL;
END;
$$;

--
-- Name: validate_post_entity(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.validate_post_entity() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF NEW.entity_type = 'hotel' THEN
        IF NOT EXISTS (SELECT 1 FROM hotels WHERE id = NEW.entity_id) THEN
            RAISE EXCEPTION 'posts.entity_id % does not reference an existing hotel', NEW.entity_id;
        END IF;
    ELSIF NEW.entity_type = 'landmark' THEN
        IF NOT EXISTS (SELECT 1 FROM landmarks WHERE id = NEW.entity_id) THEN
            RAISE EXCEPTION 'posts.entity_id % does not reference an existing landmark', NEW.entity_id;
        END IF;
    ELSIF NEW.entity_type = 'destination' THEN
        IF NOT EXISTS (SELECT 1 FROM destinations WHERE id = NEW.entity_id) THEN
            RAISE EXCEPTION 'posts.entity_id % does not reference an existing destination', NEW.entity_id;
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

--
-- Name: cars; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cars (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    destination_id uuid,
    managed_by uuid,
    name character varying(150) NOT NULL,
    brand character varying(80),
    model character varying(80),
    license_plate character varying(20),
    car_type public.car_type NOT NULL,
    seats smallint NOT NULL,
    transmission character varying(20),
    fuel_type character varying(20),
    price_per_day numeric(12,2) NOT NULL,
    with_driver boolean DEFAULT false,
    pickup_location text,
    description text,
    cover_image_url text,
    is_active boolean DEFAULT true,
    created_at timestamp with time zone DEFAULT now(),
    updated_at timestamp with time zone DEFAULT now(),
    CONSTRAINT cars_seats_check CHECK ((seats > 0))
);

--
-- Name: destinations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.destinations (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    country_id uuid NOT NULL,
    name character varying(150) NOT NULL,
    description text,
    cover_image_url text,
    is_popular boolean DEFAULT false NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    latitude numeric(10,7),
    longitude numeric(10,7)
);

--
-- Name: cars; Type: VIEW; Schema: chatbot; Owner: -
--

CREATE VIEW chatbot.cars AS
 SELECT ca.id,
    ca.name,
    ca.brand,
    ca.model,
    (ca.car_type)::text AS car_type,
    ca.seats,
    ca.transmission,
    ca.fuel_type,
    ca.with_driver,
    ca.price_per_day AS price_per_day_usd,
    d.name AS destination,
    ca.destination_id,
    ca.pickup_location,
    ca.description
   FROM (public.cars ca
     LEFT JOIN public.destinations d ON ((d.id = ca.destination_id)))
  WHERE ca.is_active;

--
-- Name: continents; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.continents (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(2) NOT NULL,
    name character varying(100) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);

--
-- Name: countries; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.countries (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    continent_id uuid NOT NULL,
    country_code character varying(3) NOT NULL,
    name character varying(100) NOT NULL,
    slug character varying(150) NOT NULL,
    image_url character varying(255),
    description text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    latitude numeric(10,7),
    longitude numeric(10,7)
);

--
-- Name: destinations; Type: VIEW; Schema: chatbot; Owner: -
--

CREATE VIEW chatbot.destinations AS
 SELECT d.id,
    d.name,
    c.name AS country,
    c.country_code,
    ct.name AS continent,
    d.is_popular,
    d.latitude,
    d.longitude,
    d.description
   FROM ((public.destinations d
     JOIN public.countries c ON ((c.id = d.country_id)))
     LEFT JOIN public.continents ct ON ((ct.id = c.continent_id)))
  WHERE d.is_active;

--
-- Name: flights; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.flights (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    flight_number character varying(20) NOT NULL,
    airline character varying(100) NOT NULL,
    airline_logo_url text,
    departure_airport_code character varying(10) NOT NULL,
    arrival_airport_code character varying(10) NOT NULL,
    departure_city character varying(100),
    arrival_city character varying(100),
    departure_time timestamp with time zone NOT NULL,
    arrival_time timestamp with time zone NOT NULL,
    duration_minutes integer,
    aircraft_type character varying(50),
    base_price numeric(12,2) NOT NULL,
    total_seats smallint NOT NULL,
    available_seats smallint NOT NULL,
    baggage_policy text,
    is_active boolean DEFAULT true,
    created_at timestamp with time zone DEFAULT now(),
    updated_at timestamp with time zone DEFAULT now(),
    CONSTRAINT flights_check CHECK ((arrival_time > departure_time))
);

--
-- Name: flights; Type: VIEW; Schema: chatbot; Owner: -
--

CREATE VIEW chatbot.flights AS
 SELECT id,
    flight_number,
    airline,
    departure_airport_code,
    arrival_airport_code,
    departure_city,
    arrival_city,
    departure_time,
    arrival_time,
    duration_minutes,
    aircraft_type,
    base_price AS base_price_usd,
    available_seats,
    baggage_policy
   FROM public.flights f
  WHERE (is_active AND (departure_time > (now() + '02:00:00'::interval)));

--
-- Name: amenities; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.amenities (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(255) NOT NULL,
    icon_url text,
    category character varying(50) NOT NULL
);

--
-- Name: hotel_amenities; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hotel_amenities (
    hotel_id uuid NOT NULL,
    amenity_id uuid NOT NULL
);

--
-- Name: hotels; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hotels (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    destination_id uuid NOT NULL,
    managed_by uuid,
    name character varying(200) NOT NULL,
    description text,
    address text NOT NULL,
    star_rating smallint NOT NULL,
    check_in_time time without time zone DEFAULT '14:00:00'::time without time zone NOT NULL,
    check_out_time time without time zone DEFAULT '12:00:00'::time without time zone NOT NULL,
    phone character varying(20),
    email character varying(255),
    cancellation_policy character varying(20) DEFAULT 'free'::character varying NOT NULL,
    cancellation_hours smallint DEFAULT 24 NOT NULL,
    breakfast_included boolean DEFAULT false NOT NULL,
    pet_friendly boolean DEFAULT false NOT NULL,
    google_place_id character varying(100),
    total_rooms smallint,
    cover_image_url text,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    latitude numeric(10,7),
    longitude numeric(10,7),
    CONSTRAINT hotels_cancellation_policy_check CHECK (((cancellation_policy)::text = ANY ((ARRAY['free'::character varying, 'partial'::character varying, 'strict'::character varying])::text[]))),
    CONSTRAINT hotels_star_rating_check CHECK (((star_rating >= 1) AND (star_rating <= 5)))
);

--
-- Name: hotel_amenities; Type: VIEW; Schema: chatbot; Owner: -
--

CREATE VIEW chatbot.hotel_amenities AS
 SELECT ha.hotel_id,
    h.name AS hotel_name,
    a.name AS amenity,
    a.category
   FROM ((public.hotel_amenities ha
     JOIN public.hotels h ON ((h.id = ha.hotel_id)))
     JOIN public.amenities a ON ((a.id = ha.amenity_id)))
  WHERE h.is_active;

--
-- Name: posts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.posts (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    entity_type character varying(30) NOT NULL,
    entity_id uuid NOT NULL,
    title character varying(300),
    content text NOT NULL,
    rating smallint,
    images jsonb,
    is_verified_booking boolean DEFAULT false NOT NULL,
    status public.post_status DEFAULT 'pending'::public.post_status NOT NULL,
    upvotes integer DEFAULT 0 NOT NULL,
    downvotes integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT posts_entity_type_check CHECK (((entity_type)::text = ANY ((ARRAY['hotel'::character varying, 'landmark'::character varying, 'destination'::character varying, 'tour'::character varying, 'car'::character varying, 'flight'::character varying])::text[]))),
    CONSTRAINT posts_rating_check CHECK (((rating >= 1) AND (rating <= 5)))
);

--
-- Name: room_types; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.room_types (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    hotel_id uuid NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    max_occupancy smallint DEFAULT 2 NOT NULL,
    bed_type character varying(60),
    area_sqm numeric(6,2),
    price_per_night numeric(12,2) NOT NULL,
    cover_image_url text,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

--
-- Name: hotels; Type: VIEW; Schema: chatbot; Owner: -
--

CREATE VIEW chatbot.hotels AS
 SELECT h.id,
    h.name,
    d.name AS destination,
    h.destination_id,
    c.name AS country,
    h.address,
    h.star_rating,
    p.min_price_usd,
    r.avg_rating,
    COALESCE(r.review_count, (0)::bigint) AS review_count,
    h.cancellation_policy,
    h.cancellation_hours,
    h.breakfast_included,
    h.pet_friendly,
    h.check_in_time,
    h.check_out_time,
    h.latitude,
    h.longitude,
    h.description
   FROM ((((public.hotels h
     JOIN public.destinations d ON ((d.id = h.destination_id)))
     JOIN public.countries c ON ((c.id = d.country_id)))
     LEFT JOIN LATERAL ( SELECT min(rt.price_per_night) AS min_price_usd
           FROM public.room_types rt
          WHERE ((rt.hotel_id = h.id) AND rt.is_active)) p ON (true))
     LEFT JOIN LATERAL ( SELECT round(avg(po.rating), 2) AS avg_rating,
            count(*) AS review_count
           FROM public.posts po
          WHERE (((po.entity_type)::text = 'hotel'::text) AND (po.entity_id = h.id) AND (po.status = 'approved'::public.post_status) AND (po.rating IS NOT NULL))) r ON (true))
  WHERE h.is_active;

--
-- Name: landmarks; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.landmarks (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    destination_id uuid NOT NULL,
    name character varying(200) NOT NULL,
    description text,
    category public.landmark_category DEFAULT 'other'::public.landmark_category NOT NULL,
    cover_image_url text,
    opening_hours character varying(200),
    entry_fee numeric(12,2),
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now(),
    address text,
    phone character varying(30),
    website character varying(255),
    latitude numeric(10,7),
    longitude numeric(10,7)
);

--
-- Name: landmarks; Type: VIEW; Schema: chatbot; Owner: -
--

CREATE VIEW chatbot.landmarks AS
 SELECT l.id,
    l.name,
    d.name AS destination,
    l.destination_id,
    (l.category)::text AS category,
    l.address,
    l.opening_hours,
    l.entry_fee AS entry_fee_usd,
    l.latitude,
    l.longitude,
    l.description
   FROM (public.landmarks l
     JOIN public.destinations d ON ((d.id = l.destination_id)))
  WHERE l.is_active;

--
-- Name: room_types; Type: VIEW; Schema: chatbot; Owner: -
--

CREATE VIEW chatbot.room_types AS
 SELECT rt.id,
    rt.hotel_id,
    h.name AS hotel_name,
    rt.name,
    rt.max_occupancy,
    rt.bed_type,
    rt.area_sqm,
    rt.price_per_night AS price_per_night_usd,
    rt.description
   FROM (public.room_types rt
     JOIN public.hotels h ON ((h.id = rt.hotel_id)))
  WHERE (rt.is_active AND h.is_active);

--
-- Name: tours; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tours (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    destination_id uuid,
    name character varying(255) NOT NULL,
    slug character varying(255),
    description text,
    itinerary text,
    duration_days smallint NOT NULL,
    duration_nights smallint,
    max_participants smallint,
    min_participants smallint DEFAULT 1,
    price_adult numeric(12,2) NOT NULL,
    price_child numeric(12,2),
    departure_date date,
    return_date date,
    departure_location character varying(255),
    cover_image_url text,
    highlights jsonb,
    included text,
    excluded text,
    is_active boolean DEFAULT true,
    created_at timestamp with time zone DEFAULT now(),
    updated_at timestamp with time zone DEFAULT now()
);

--
-- Name: tours; Type: VIEW; Schema: chatbot; Owner: -
--

CREATE VIEW chatbot.tours AS
 SELECT t.id,
    t.name,
    d.name AS destination,
    t.destination_id,
    t.duration_days,
    t.duration_nights,
    t.price_adult AS price_adult_usd,
    COALESCE(t.price_child, t.price_adult) AS price_child_usd,
    t.departure_date,
    t.return_date,
    t.departure_location,
    t.max_participants,
    r.avg_rating,
    COALESCE(r.review_count, (0)::bigint) AS review_count,
    (t.highlights)::text AS highlights,
    t.included,
    t.excluded,
    t.description
   FROM ((public.tours t
     LEFT JOIN public.destinations d ON ((d.id = t.destination_id)))
     LEFT JOIN LATERAL ( SELECT round(avg(po.rating), 2) AS avg_rating,
            count(*) AS review_count
           FROM public.posts po
          WHERE (((po.entity_type)::text = 'tour'::text) AND (po.entity_id = t.id) AND (po.status = 'approved'::public.post_status) AND (po.rating IS NOT NULL))) r ON (true))
  WHERE t.is_active;

--
-- Name: ai_jobs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ai_jobs (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    job_type character varying(40) NOT NULL,
    payload jsonb NOT NULL,
    status character varying(20) DEFAULT 'pending'::character varying,
    attempts smallint DEFAULT 0,
    error_message text,
    started_at timestamp with time zone,
    finished_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now()
);

--
-- Name: ai_summaries; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ai_summaries (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    entity_type character varying(20) NOT NULL,
    entity_id uuid NOT NULL,
    summary_type character varying(30) NOT NULL,
    language character(2) DEFAULT 'vi'::bpchar,
    content text NOT NULL,
    based_on_count integer,
    model_version character varying(30),
    generated_at timestamp with time zone DEFAULT now()
);

--
-- Name: audit_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audit_logs (
    id bigint NOT NULL,
    user_id uuid,
    user_email character varying(255),
    action character varying(80) NOT NULL,
    http_method character varying(10),
    path text,
    entity_type character varying(40),
    entity_id character varying(64),
    status_code integer,
    ip_address inet,
    user_agent text,
    details jsonb,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

--
-- Name: audit_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.audit_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: audit_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.audit_logs_id_seq OWNED BY public.audit_logs.id;

--
-- Name: car_bookings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.car_bookings (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    order_id uuid,
    user_id uuid NOT NULL,
    car_id uuid NOT NULL,
    pickup_date timestamp with time zone NOT NULL,
    return_date timestamp with time zone NOT NULL,
    pickup_location text,
    return_location text,
    driver_license_no character varying(50),
    total_price numeric(12,2) NOT NULL,
    discount_amount numeric(12,2) DEFAULT 0,
    special_requests text,
    status public.booking_status DEFAULT 'pending'::public.booking_status,
    created_at timestamp with time zone DEFAULT now(),
    updated_at timestamp with time zone DEFAULT now(),
    CONSTRAINT car_bookings_check CHECK ((return_date > pickup_date))
);

--
-- Name: cart_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cart_items (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    cart_id uuid NOT NULL,
    item_type public.cart_item_type NOT NULL,
    item_id uuid NOT NULL,
    item_snapshot jsonb,
    quantity smallint DEFAULT 1,
    check_in_date date,
    check_out_date date,
    room_type_id uuid,
    departure_date date,
    unit_price numeric(12,2) NOT NULL,
    total_price numeric(12,2) NOT NULL,
    added_at timestamp with time zone DEFAULT now(),
    CONSTRAINT cart_items_quantity_check CHECK ((quantity > 0))
);

--
-- Name: carts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.carts (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid,
    session_token character varying(64),
    created_at timestamp with time zone DEFAULT now(),
    updated_at timestamp with time zone DEFAULT now(),
    CONSTRAINT carts_check CHECK (((user_id IS NOT NULL) OR (session_token IS NOT NULL)))
);

--
-- Name: chat_feedback; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.chat_feedback (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    message_id uuid NOT NULL,
    user_id uuid,
    rating smallint,
    is_helpful boolean,
    comment text,
    created_at timestamp with time zone DEFAULT now(),
    CONSTRAINT chat_feedback_rating_check CHECK (((rating >= 1) AND (rating <= 5)))
);

--
-- Name: chat_messages; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.chat_messages (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    session_id uuid NOT NULL,
    role character varying(20) NOT NULL,
    content text NOT NULL,
    tool_calls jsonb,
    retrieved_chunks jsonb,
    model_name character varying(50),
    prompt_tokens integer,
    completion_tokens integer,
    latency_ms integer,
    error_message text,
    created_at timestamp with time zone DEFAULT now()
);

--
-- Name: chat_sessions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.chat_sessions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid,
    session_token character varying(64),
    title character varying(255),
    channel character varying(20) DEFAULT 'web'::character varying,
    context_type character varying(30),
    context_id uuid,
    total_tokens integer DEFAULT 0,
    is_active boolean DEFAULT true,
    created_at timestamp with time zone DEFAULT now(),
    updated_at timestamp with time zone DEFAULT now()
);

--
-- Name: comments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.comments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    post_id uuid NOT NULL,
    user_id uuid NOT NULL,
    parent_comment_id uuid,
    content text NOT NULL,
    status character varying(20) DEFAULT 'active'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT comments_status_check CHECK (((status)::text = ANY ((ARRAY['active'::character varying, 'deleted'::character varying, 'flagged'::character varying])::text[])))
);

--
-- Name: contact_messages; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.contact_messages (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid,
    full_name character varying(150) NOT NULL,
    email character varying(150) NOT NULL,
    phone character varying(30),
    subject character varying(255),
    message text NOT NULL,
    status public.contact_status DEFAULT 'new'::public.contact_status,
    ip_address inet,
    replied_by uuid,
    reply_content text,
    replied_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now(),
    updated_at timestamp with time zone DEFAULT now()
);

--
-- Name: favorites; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.favorites (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    item_type public.cart_item_type NOT NULL,
    item_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now()
);

--
-- Name: flight_bookings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.flight_bookings (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    order_id uuid,
    user_id uuid NOT NULL,
    flight_id uuid NOT NULL,
    seat_id uuid,
    passenger_name character varying(150) NOT NULL,
    passenger_email character varying(150),
    passenger_phone character varying(30),
    passport_no character varying(30),
    baggage_kg smallint,
    total_price numeric(12,2) NOT NULL,
    status public.booking_status DEFAULT 'pending'::public.booking_status,
    created_at timestamp with time zone DEFAULT now(),
    updated_at timestamp with time zone DEFAULT now()
);

--
-- Name: flight_seats; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.flight_seats (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    flight_id uuid NOT NULL,
    seat_number character varying(10) NOT NULL,
    seat_class public.flight_seat_class NOT NULL,
    price numeric(12,2) NOT NULL,
    status public.flight_seat_status DEFAULT 'available'::public.flight_seat_status
);

--
-- Name: fraud_scores; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fraud_scores (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    entity_type character varying(20) NOT NULL,
    entity_id uuid NOT NULL,
    risk_score real NOT NULL,
    risk_level character varying(10),
    reasons jsonb,
    action_taken character varying(30),
    model_version character varying(30),
    created_at timestamp with time zone DEFAULT now()
);

--
-- Name: hotel_bookings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hotel_bookings (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    order_id uuid NOT NULL,
    user_id uuid NOT NULL,
    hotel_id uuid NOT NULL,
    room_type_id uuid NOT NULL,
    room_id uuid,
    check_in_date date NOT NULL,
    check_out_date date NOT NULL,
    num_nights smallint GENERATED ALWAYS AS ((check_out_date - check_in_date)) STORED NOT NULL,
    num_adults smallint DEFAULT 1 NOT NULL,
    num_children smallint DEFAULT 0 NOT NULL,
    total_price numeric(14,2) NOT NULL,
    discount_amount numeric(14,2) DEFAULT 0 NOT NULL,
    special_requests text,
    status public.booking_status DEFAULT 'pending'::public.booking_status NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    refund_amount numeric(12,2),
    refund_reason text,
    refunded_at timestamp with time zone,
    CONSTRAINT chk_adults CHECK ((num_adults >= 1)),
    CONSTRAINT chk_dates CHECK ((check_out_date > check_in_date))
);

--
-- Name: hotel_images; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hotel_images (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    hotel_id uuid NOT NULL,
    url text NOT NULL,
    caption character varying(255),
    sort_order integer DEFAULT 0 NOT NULL,
    created_by uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

--
-- Name: image_ai_metadata; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.image_ai_metadata (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    entity_type character varying(20) NOT NULL,
    entity_id uuid NOT NULL,
    image_url text NOT NULL,
    tags text[],
    dominant_colors jsonb,
    nsfw_score real,
    is_approved boolean,
    caption_vi text,
    embedding jsonb,
    model_version character varying(30),
    analyzed_at timestamp with time zone DEFAULT now()
);

--
-- Name: invoices; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.invoices (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    invoice_number character varying(50) NOT NULL,
    order_id uuid NOT NULL,
    payment_id uuid,
    user_id uuid NOT NULL,
    billing_name character varying(150),
    billing_address text,
    billing_tax_code character varying(30),
    subtotal numeric(12,2) NOT NULL,
    discount_total numeric(12,2) DEFAULT 0,
    tax_amount numeric(12,2) DEFAULT 0,
    total_amount numeric(12,2) NOT NULL,
    currency_code character(3) DEFAULT 'VND'::bpchar,
    pdf_url text,
    issued_at timestamp with time zone DEFAULT now(),
    created_at timestamp with time zone DEFAULT now()
);

--
-- Name: item_embeddings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.item_embeddings (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    entity_type character varying(20) NOT NULL,
    entity_id uuid NOT NULL,
    embedding jsonb NOT NULL,
    model_version character varying(30),
    updated_at timestamp with time zone DEFAULT now()
);

--
-- Name: itineraries; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.itineraries (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid,
    title character varying(255),
    destination_id uuid,
    start_date date,
    end_date date,
    total_budget numeric(12,2),
    party_size smallint,
    prompt text,
    generated_by character varying(20) DEFAULT 'ai'::character varying,
    model_version character varying(30),
    status character varying(20) DEFAULT 'draft'::character varying,
    created_at timestamp with time zone DEFAULT now(),
    updated_at timestamp with time zone DEFAULT now()
);

--
-- Name: itinerary_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.itinerary_items (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    itinerary_id uuid NOT NULL,
    day_number smallint NOT NULL,
    start_time time without time zone,
    end_time time without time zone,
    entity_type character varying(20),
    entity_id uuid,
    title character varying(255),
    notes text,
    estimated_cost numeric(12,2),
    sort_order smallint,
    created_at timestamp with time zone DEFAULT now()
);

--
-- Name: knowledge_chunks; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.knowledge_chunks (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    document_id uuid NOT NULL,
    chunk_index integer NOT NULL,
    content text NOT NULL,
    token_count integer,
    embedding jsonb,
    metadata jsonb,
    created_at timestamp with time zone DEFAULT now()
);

--
-- Name: knowledge_documents; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.knowledge_documents (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    source_type character varying(30) NOT NULL,
    source_id uuid,
    title character varying(255) NOT NULL,
    content text NOT NULL,
    url text,
    language character(2) DEFAULT 'vi'::bpchar,
    version integer DEFAULT 1,
    is_active boolean DEFAULT true,
    indexed_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now(),
    updated_at timestamp with time zone DEFAULT now(),
    content_sha256 character(64),
    embedding_model character varying(100)
);

--
-- Name: notifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notifications (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    type character varying(40) NOT NULL,
    title character varying(200) NOT NULL,
    body text,
    link text,
    entity_type character varying(30),
    entity_id uuid,
    is_read boolean DEFAULT false NOT NULL,
    read_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

--
-- Name: orders; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.orders (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    order_code character varying(30) NOT NULL,
    subtotal numeric(14,2) DEFAULT 0 NOT NULL,
    discount_total numeric(14,2) DEFAULT 0 NOT NULL,
    tax_amount numeric(14,2) DEFAULT 0 NOT NULL,
    currency_code character(3) DEFAULT 'VND'::bpchar NOT NULL,
    loyalty_points_used integer DEFAULT 0 NOT NULL,
    loyalty_points_earned integer DEFAULT 0 NOT NULL,
    invoice_url text,
    status public.order_status DEFAULT 'pending'::public.order_status NOT NULL,
    notes text,
    cancelled_at timestamp with time zone,
    cancel_reason text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    total_amount numeric(12,2) NOT NULL
);

--
-- Name: password_reset_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.password_reset_tokens (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    token_hash character varying(255) NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    used_at timestamp with time zone,
    ip_address inet,
    created_at timestamp with time zone DEFAULT now()
);

--
-- Name: payments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    order_id uuid NOT NULL,
    payment_method public.payment_method NOT NULL,
    amount numeric(14,2) NOT NULL,
    currency_code character(3) DEFAULT 'VND'::bpchar NOT NULL,
    transaction_id character varying(200),
    gateway_response jsonb,
    status public.payment_status DEFAULT 'pending'::public.payment_status NOT NULL,
    paid_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

--
-- Name: permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.permissions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(80) NOT NULL,
    name character varying(150) NOT NULL,
    description text,
    created_at timestamp with time zone DEFAULT now()
);

--
-- Name: post_reactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.post_reactions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    post_id uuid NOT NULL,
    user_id uuid NOT NULL,
    reaction_type public.reaction_type NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

--
-- Name: price_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.price_history (
    id bigint NOT NULL,
    entity_type character varying(20) NOT NULL,
    entity_id uuid NOT NULL,
    price numeric(12,2) NOT NULL,
    currency_code character(3) DEFAULT 'VND'::bpchar,
    check_date date NOT NULL,
    recorded_at timestamp with time zone DEFAULT now()
);

--
-- Name: price_history_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.price_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: price_history_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.price_history_id_seq OWNED BY public.price_history.id;

--
-- Name: price_predictions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.price_predictions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    entity_type character varying(20) NOT NULL,
    entity_id uuid NOT NULL,
    target_date date NOT NULL,
    predicted_price numeric(12,2) NOT NULL,
    confidence real,
    recommendation character varying(20),
    model_version character varying(30),
    generated_at timestamp with time zone DEFAULT now()
);

--
-- Name: recommendations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.recommendations (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    entity_type character varying(20) NOT NULL,
    entity_id uuid NOT NULL,
    score real NOT NULL,
    reason character varying(50),
    algorithm character varying(30),
    generated_at timestamp with time zone DEFAULT now(),
    expires_at timestamp with time zone
);

--
-- Name: refresh_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.refresh_tokens (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    token_hash character varying(255) NOT NULL,
    device_info character varying(255),
    ip_address inet,
    expires_at timestamp with time zone NOT NULL,
    revoked_at timestamp with time zone,
    replaced_by uuid,
    created_at timestamp with time zone DEFAULT now()
);

--
-- Name: review_ai_analysis; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.review_ai_analysis (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    post_id uuid NOT NULL,
    sentiment character varying(20),
    sentiment_score real,
    topics text[],
    aspects jsonb,
    language character(2),
    is_toxic boolean DEFAULT false,
    is_spam boolean DEFAULT false,
    model_version character varying(30),
    analyzed_at timestamp with time zone DEFAULT now()
);

--
-- Name: role_permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.role_permissions (
    role_id uuid NOT NULL,
    permission_id uuid NOT NULL,
    assigned_at timestamp with time zone DEFAULT now()
);

--
-- Name: roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.roles (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(50) NOT NULL,
    description text,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

--
-- Name: room_availability; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.room_availability (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    room_id uuid NOT NULL,
    date date NOT NULL,
    status public.availability_status DEFAULT 'available'::public.availability_status NOT NULL,
    blocked_reason character varying(200)
);

--
-- Name: room_type_amenities; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.room_type_amenities (
    room_type_id uuid NOT NULL,
    amenity_id uuid NOT NULL
);

--
-- Name: rooms; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.rooms (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    room_type_id uuid NOT NULL,
    room_number character varying(10) NOT NULL,
    floor smallint,
    status public.room_status DEFAULT 'available'::public.room_status NOT NULL
);

--
-- Name: search_queries; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.search_queries (
    id bigint NOT NULL,
    user_id uuid,
    query_text text NOT NULL,
    query_embedding jsonb,
    filters jsonb,
    result_count integer,
    clicked_entity_type character varying(20),
    clicked_entity_id uuid,
    created_at timestamp with time zone DEFAULT now()
);

--
-- Name: search_queries_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.search_queries_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: search_queries_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.search_queries_id_seq OWNED BY public.search_queries.id;

--
-- Name: similar_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.similar_items (
    entity_type character varying(20) NOT NULL,
    entity_id uuid NOT NULL,
    similar_entity_id uuid NOT NULL,
    similarity_score real NOT NULL,
    generated_at timestamp with time zone DEFAULT now()
);

--
-- Name: tour_bookings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tour_bookings (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    order_id uuid,
    user_id uuid NOT NULL,
    tour_id uuid NOT NULL,
    num_adults smallint DEFAULT 1 NOT NULL,
    num_children smallint DEFAULT 0,
    departure_date date NOT NULL,
    contact_name character varying(150),
    contact_phone character varying(30),
    contact_email character varying(150),
    total_price numeric(12,2) NOT NULL,
    discount_amount numeric(12,2) DEFAULT 0,
    special_requests text,
    status public.booking_status DEFAULT 'pending'::public.booking_status,
    created_at timestamp with time zone DEFAULT now(),
    updated_at timestamp with time zone DEFAULT now()
);

--
-- Name: tour_cars; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tour_cars (
    tour_id uuid NOT NULL,
    car_id uuid NOT NULL,
    usage_day smallint
);

--
-- Name: tour_flights; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tour_flights (
    tour_id uuid NOT NULL,
    flight_id uuid NOT NULL,
    leg character varying(20)
);

--
-- Name: tour_hotels; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tour_hotels (
    tour_id uuid NOT NULL,
    hotel_id uuid NOT NULL,
    check_in_day smallint,
    nights smallint
);

--
-- Name: user_interactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_interactions (
    id bigint NOT NULL,
    user_id uuid,
    session_token character varying(64),
    entity_type character varying(20) NOT NULL,
    entity_id uuid NOT NULL,
    action character varying(20) NOT NULL,
    dwell_seconds integer,
    weight real DEFAULT 1.0,
    metadata jsonb,
    created_at timestamp with time zone DEFAULT now()
);

--
-- Name: user_interactions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_interactions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: user_interactions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_interactions_id_seq OWNED BY public.user_interactions.id;

--
-- Name: user_oauth_accounts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_oauth_accounts (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    provider public.oauth_provider NOT NULL,
    provider_user_id character varying(255) NOT NULL,
    email character varying(255),
    display_name character varying(255),
    avatar_url text,
    access_token text,
    refresh_token text,
    token_expires_at timestamp with time zone,
    linked_at timestamp with time zone DEFAULT now()
);

--
-- Name: user_preferences; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_preferences (
    user_id uuid NOT NULL,
    travel_styles text[],
    preferred_budget numeric(12,2),
    budget_currency character(3) DEFAULT 'VND'::bpchar,
    preferred_countries uuid[],
    preferred_amenities uuid[],
    trip_duration_days smallint,
    with_children boolean DEFAULT false,
    with_pets boolean DEFAULT false,
    updated_at timestamp with time zone DEFAULT now()
);

--
-- Name: user_roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_roles (
    user_id uuid NOT NULL,
    role_id uuid NOT NULL,
    assigned_at timestamp with time zone DEFAULT now() NOT NULL,
    assigned_by uuid
);

--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    email character varying(255) NOT NULL,
    password_hash character varying(255) NOT NULL,
    full_name character varying(150) NOT NULL,
    phone character varying(20),
    avatar_url text,
    date_of_birth date,
    gender public.user_gender,
    loyalty_points integer DEFAULT 0 NOT NULL,
    is_verified boolean DEFAULT false NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    last_login_at timestamp with time zone,
    deleted_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);

--
-- Name: audit_logs id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs ALTER COLUMN id SET DEFAULT nextval('public.audit_logs_id_seq'::regclass);

--
-- Name: price_history id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.price_history ALTER COLUMN id SET DEFAULT nextval('public.price_history_id_seq'::regclass);

--
-- Name: search_queries id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.search_queries ALTER COLUMN id SET DEFAULT nextval('public.search_queries_id_seq'::regclass);

--
-- Name: user_interactions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_interactions ALTER COLUMN id SET DEFAULT nextval('public.user_interactions_id_seq'::regclass);

--
-- Name: ai_jobs ai_jobs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ai_jobs
    ADD CONSTRAINT ai_jobs_pkey PRIMARY KEY (id);

--
-- Name: ai_summaries ai_summaries_entity_type_entity_id_summary_type_language_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ai_summaries
    ADD CONSTRAINT ai_summaries_entity_type_entity_id_summary_type_language_key UNIQUE (entity_type, entity_id, summary_type, language);

--
-- Name: ai_summaries ai_summaries_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ai_summaries
    ADD CONSTRAINT ai_summaries_pkey PRIMARY KEY (id);

--
-- Name: amenities amenities_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.amenities
    ADD CONSTRAINT amenities_pkey PRIMARY KEY (id);

--
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);

--
-- Name: car_bookings car_bookings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.car_bookings
    ADD CONSTRAINT car_bookings_pkey PRIMARY KEY (id);

--
-- Name: cars cars_license_plate_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cars
    ADD CONSTRAINT cars_license_plate_key UNIQUE (license_plate);

--
-- Name: cars cars_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cars
    ADD CONSTRAINT cars_pkey PRIMARY KEY (id);

--
-- Name: cart_items cart_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT cart_items_pkey PRIMARY KEY (id);

--
-- Name: carts carts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.carts
    ADD CONSTRAINT carts_pkey PRIMARY KEY (id);

--
-- Name: carts carts_session_token_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.carts
    ADD CONSTRAINT carts_session_token_key UNIQUE (session_token);

--
-- Name: carts carts_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.carts
    ADD CONSTRAINT carts_user_id_key UNIQUE (user_id);

--
-- Name: chat_feedback chat_feedback_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.chat_feedback
    ADD CONSTRAINT chat_feedback_pkey PRIMARY KEY (id);

--
-- Name: chat_messages chat_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.chat_messages
    ADD CONSTRAINT chat_messages_pkey PRIMARY KEY (id);

--
-- Name: chat_sessions chat_sessions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.chat_sessions
    ADD CONSTRAINT chat_sessions_pkey PRIMARY KEY (id);

--
-- Name: chat_sessions chat_sessions_session_token_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.chat_sessions
    ADD CONSTRAINT chat_sessions_session_token_key UNIQUE (session_token);

--
-- Name: comments comments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT comments_pkey PRIMARY KEY (id);

--
-- Name: contact_messages contact_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contact_messages
    ADD CONSTRAINT contact_messages_pkey PRIMARY KEY (id);

--
-- Name: continents continents_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.continents
    ADD CONSTRAINT continents_code_key UNIQUE (code);

--
-- Name: continents continents_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.continents
    ADD CONSTRAINT continents_name_key UNIQUE (name);

--
-- Name: continents continents_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.continents
    ADD CONSTRAINT continents_pkey PRIMARY KEY (id);

--
-- Name: countries countries_country_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.countries
    ADD CONSTRAINT countries_country_code_key UNIQUE (country_code);

--
-- Name: countries countries_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.countries
    ADD CONSTRAINT countries_pkey PRIMARY KEY (id);

--
-- Name: countries countries_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.countries
    ADD CONSTRAINT countries_slug_key UNIQUE (slug);

--
-- Name: destinations destinations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.destinations
    ADD CONSTRAINT destinations_pkey PRIMARY KEY (id);

--
-- Name: favorites favorites_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.favorites
    ADD CONSTRAINT favorites_pkey PRIMARY KEY (id);

--
-- Name: favorites favorites_user_id_item_type_item_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.favorites
    ADD CONSTRAINT favorites_user_id_item_type_item_id_key UNIQUE (user_id, item_type, item_id);

--
-- Name: flight_bookings flight_bookings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flight_bookings
    ADD CONSTRAINT flight_bookings_pkey PRIMARY KEY (id);

--
-- Name: flight_seats flight_seats_flight_id_seat_number_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flight_seats
    ADD CONSTRAINT flight_seats_flight_id_seat_number_key UNIQUE (flight_id, seat_number);

--
-- Name: flight_seats flight_seats_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flight_seats
    ADD CONSTRAINT flight_seats_pkey PRIMARY KEY (id);

--
-- Name: flights flights_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flights
    ADD CONSTRAINT flights_pkey PRIMARY KEY (id);

--
-- Name: fraud_scores fraud_scores_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fraud_scores
    ADD CONSTRAINT fraud_scores_pkey PRIMARY KEY (id);

--
-- Name: hotel_amenities hotel_amenities_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hotel_amenities
    ADD CONSTRAINT hotel_amenities_pkey PRIMARY KEY (hotel_id, amenity_id);

--
-- Name: hotel_bookings hotel_bookings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hotel_bookings
    ADD CONSTRAINT hotel_bookings_pkey PRIMARY KEY (id);

--
-- Name: hotel_images hotel_images_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hotel_images
    ADD CONSTRAINT hotel_images_pkey PRIMARY KEY (id);

--
-- Name: hotels hotels_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hotels
    ADD CONSTRAINT hotels_pkey PRIMARY KEY (id);

--
-- Name: image_ai_metadata image_ai_metadata_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.image_ai_metadata
    ADD CONSTRAINT image_ai_metadata_pkey PRIMARY KEY (id);

--
-- Name: invoices invoices_invoice_number_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT invoices_invoice_number_key UNIQUE (invoice_number);

--
-- Name: invoices invoices_order_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT invoices_order_id_key UNIQUE (order_id);

--
-- Name: invoices invoices_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT invoices_pkey PRIMARY KEY (id);

--
-- Name: item_embeddings item_embeddings_entity_type_entity_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_embeddings
    ADD CONSTRAINT item_embeddings_entity_type_entity_id_key UNIQUE (entity_type, entity_id);

--
-- Name: item_embeddings item_embeddings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_embeddings
    ADD CONSTRAINT item_embeddings_pkey PRIMARY KEY (id);

--
-- Name: itineraries itineraries_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.itineraries
    ADD CONSTRAINT itineraries_pkey PRIMARY KEY (id);

--
-- Name: itinerary_items itinerary_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.itinerary_items
    ADD CONSTRAINT itinerary_items_pkey PRIMARY KEY (id);

--
-- Name: knowledge_chunks knowledge_chunks_document_id_chunk_index_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.knowledge_chunks
    ADD CONSTRAINT knowledge_chunks_document_id_chunk_index_key UNIQUE (document_id, chunk_index);

--
-- Name: knowledge_chunks knowledge_chunks_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.knowledge_chunks
    ADD CONSTRAINT knowledge_chunks_pkey PRIMARY KEY (id);

--
-- Name: knowledge_documents knowledge_documents_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.knowledge_documents
    ADD CONSTRAINT knowledge_documents_pkey PRIMARY KEY (id);

--
-- Name: landmarks landmarks_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.landmarks
    ADD CONSTRAINT landmarks_pkey PRIMARY KEY (id);

--
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);

--
-- Name: orders orders_order_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_order_code_key UNIQUE (order_code);

--
-- Name: orders orders_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_pkey PRIMARY KEY (id);

--
-- Name: password_reset_tokens password_reset_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_pkey PRIMARY KEY (id);

--
-- Name: password_reset_tokens password_reset_tokens_token_hash_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_token_hash_key UNIQUE (token_hash);

--
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);

--
-- Name: payments payments_transaction_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_transaction_id_key UNIQUE (transaction_id);

--
-- Name: permissions permissions_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT permissions_code_key UNIQUE (code);

--
-- Name: permissions permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT permissions_pkey PRIMARY KEY (id);

--
-- Name: post_reactions post_reactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.post_reactions
    ADD CONSTRAINT post_reactions_pkey PRIMARY KEY (id);

--
-- Name: post_reactions post_reactions_post_id_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.post_reactions
    ADD CONSTRAINT post_reactions_post_id_user_id_key UNIQUE (post_id, user_id);

--
-- Name: posts posts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.posts
    ADD CONSTRAINT posts_pkey PRIMARY KEY (id);

--
-- Name: price_history price_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.price_history
    ADD CONSTRAINT price_history_pkey PRIMARY KEY (id);

--
-- Name: price_predictions price_predictions_entity_type_entity_id_target_date_model_v_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.price_predictions
    ADD CONSTRAINT price_predictions_entity_type_entity_id_target_date_model_v_key UNIQUE (entity_type, entity_id, target_date, model_version);

--
-- Name: price_predictions price_predictions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.price_predictions
    ADD CONSTRAINT price_predictions_pkey PRIMARY KEY (id);

--
-- Name: recommendations recommendations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recommendations
    ADD CONSTRAINT recommendations_pkey PRIMARY KEY (id);

--
-- Name: recommendations recommendations_user_id_entity_type_entity_id_algorithm_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recommendations
    ADD CONSTRAINT recommendations_user_id_entity_type_entity_id_algorithm_key UNIQUE (user_id, entity_type, entity_id, algorithm);

--
-- Name: refresh_tokens refresh_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);

--
-- Name: refresh_tokens refresh_tokens_token_hash_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_token_hash_key UNIQUE (token_hash);

--
-- Name: review_ai_analysis review_ai_analysis_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.review_ai_analysis
    ADD CONSTRAINT review_ai_analysis_pkey PRIMARY KEY (id);

--
-- Name: review_ai_analysis review_ai_analysis_post_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.review_ai_analysis
    ADD CONSTRAINT review_ai_analysis_post_id_key UNIQUE (post_id);

--
-- Name: role_permissions role_permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT role_permissions_pkey PRIMARY KEY (role_id, permission_id);

--
-- Name: roles roles_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_name_key UNIQUE (name);

--
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);

--
-- Name: room_availability room_availability_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.room_availability
    ADD CONSTRAINT room_availability_pkey PRIMARY KEY (id);

--
-- Name: room_availability room_availability_room_id_date_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.room_availability
    ADD CONSTRAINT room_availability_room_id_date_key UNIQUE (room_id, date);

--
-- Name: room_type_amenities room_type_amenities_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.room_type_amenities
    ADD CONSTRAINT room_type_amenities_pkey PRIMARY KEY (room_type_id, amenity_id);

--
-- Name: room_types room_types_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.room_types
    ADD CONSTRAINT room_types_pkey PRIMARY KEY (id);

--
-- Name: rooms rooms_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rooms
    ADD CONSTRAINT rooms_pkey PRIMARY KEY (id);

--
-- Name: rooms rooms_room_type_id_room_number_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rooms
    ADD CONSTRAINT rooms_room_type_id_room_number_key UNIQUE (room_type_id, room_number);

--
-- Name: search_queries search_queries_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.search_queries
    ADD CONSTRAINT search_queries_pkey PRIMARY KEY (id);

--
-- Name: similar_items similar_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.similar_items
    ADD CONSTRAINT similar_items_pkey PRIMARY KEY (entity_type, entity_id, similar_entity_id);

--
-- Name: tour_bookings tour_bookings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tour_bookings
    ADD CONSTRAINT tour_bookings_pkey PRIMARY KEY (id);

--
-- Name: tour_cars tour_cars_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tour_cars
    ADD CONSTRAINT tour_cars_pkey PRIMARY KEY (tour_id, car_id);

--
-- Name: tour_flights tour_flights_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tour_flights
    ADD CONSTRAINT tour_flights_pkey PRIMARY KEY (tour_id, flight_id);

--
-- Name: tour_hotels tour_hotels_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tour_hotels
    ADD CONSTRAINT tour_hotels_pkey PRIMARY KEY (tour_id, hotel_id);

--
-- Name: tours tours_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tours
    ADD CONSTRAINT tours_pkey PRIMARY KEY (id);

--
-- Name: tours tours_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tours
    ADD CONSTRAINT tours_slug_key UNIQUE (slug);

--
-- Name: room_availability uq_room_availability_room_date; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.room_availability
    ADD CONSTRAINT uq_room_availability_room_date UNIQUE (room_id, date);

--
-- Name: user_interactions user_interactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_interactions
    ADD CONSTRAINT user_interactions_pkey PRIMARY KEY (id);

--
-- Name: user_oauth_accounts user_oauth_accounts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_oauth_accounts
    ADD CONSTRAINT user_oauth_accounts_pkey PRIMARY KEY (id);

--
-- Name: user_oauth_accounts user_oauth_accounts_provider_provider_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_oauth_accounts
    ADD CONSTRAINT user_oauth_accounts_provider_provider_user_id_key UNIQUE (provider, provider_user_id);

--
-- Name: user_preferences user_preferences_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_preferences
    ADD CONSTRAINT user_preferences_pkey PRIMARY KEY (user_id);

--
-- Name: user_roles user_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_pkey PRIMARY KEY (user_id, role_id);

--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);

--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);

--
-- Name: amenities_name_lower_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX amenities_name_lower_key ON public.amenities USING btree (lower((name)::text));

--
-- Name: idx_ai_jobs_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ai_jobs_status ON public.ai_jobs USING btree (status, created_at);

--
-- Name: idx_audit_logs_action; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_logs_action ON public.audit_logs USING btree (action);

--
-- Name: idx_audit_logs_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_logs_time ON public.audit_logs USING btree (created_at DESC);

--
-- Name: idx_audit_logs_user_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_logs_user_time ON public.audit_logs USING btree (user_id, created_at DESC);

--
-- Name: idx_avail_date_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_avail_date_status ON public.room_availability USING btree (date, status);

--
-- Name: idx_avail_room_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_avail_room_date ON public.room_availability USING btree (room_id, date);

--
-- Name: idx_car_bookings_car_dates; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_car_bookings_car_dates ON public.car_bookings USING btree (car_id, pickup_date, return_date);

--
-- Name: idx_car_bookings_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_car_bookings_user ON public.car_bookings USING btree (user_id);

--
-- Name: idx_cars_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cars_active ON public.cars USING btree (is_active) WHERE is_active;

--
-- Name: idx_cars_destination; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cars_destination ON public.cars USING btree (destination_id);

--
-- Name: idx_cart_items_cart; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cart_items_cart ON public.cart_items USING btree (cart_id);

--
-- Name: idx_chat_messages_session; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_chat_messages_session ON public.chat_messages USING btree (session_id, created_at);

--
-- Name: idx_chat_sessions_updated; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_chat_sessions_updated ON public.chat_sessions USING btree (updated_at DESC);

--
-- Name: idx_chat_sessions_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_chat_sessions_user ON public.chat_sessions USING btree (user_id);

--
-- Name: idx_chunks_content_trgm; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_chunks_content_trgm ON public.knowledge_chunks USING gin (content public.gin_trgm_ops);

--
-- Name: idx_comments_parent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_comments_parent ON public.comments USING btree (parent_comment_id) WHERE (parent_comment_id IS NOT NULL);

--
-- Name: idx_comments_post; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_comments_post ON public.comments USING btree (post_id);

--
-- Name: idx_comments_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_comments_user ON public.comments USING btree (user_id);

--
-- Name: idx_contact_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_contact_email ON public.contact_messages USING btree (email);

--
-- Name: idx_contact_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_contact_status ON public.contact_messages USING btree (status, created_at DESC);

--
-- Name: idx_countries_continent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_countries_continent ON public.countries USING btree (continent_id);

--
-- Name: idx_countries_country_code_lower; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_countries_country_code_lower ON public.countries USING btree (lower((country_code)::text));

--
-- Name: idx_countries_slug_lower; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_countries_slug_lower ON public.countries USING btree (lower((slug)::text));

--
-- Name: idx_destinations_country; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_destinations_country ON public.destinations USING btree (country_id);

--
-- Name: idx_destinations_name_trgm; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_destinations_name_trgm ON public.destinations USING gin (name public.gin_trgm_ops);

--
-- Name: idx_destinations_popular; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_destinations_popular ON public.destinations USING btree (is_popular) WHERE (is_popular = true);

--
-- Name: idx_favorites_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_favorites_user ON public.favorites USING btree (user_id);

--
-- Name: idx_flight_bookings_flight; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_flight_bookings_flight ON public.flight_bookings USING btree (flight_id);

--
-- Name: idx_flight_bookings_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_flight_bookings_user ON public.flight_bookings USING btree (user_id);

--
-- Name: idx_flight_seats_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_flight_seats_status ON public.flight_seats USING btree (flight_id, status);

--
-- Name: idx_flights_number; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_flights_number ON public.flights USING btree (flight_number);

--
-- Name: idx_flights_route_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_flights_route_time ON public.flights USING btree (departure_airport_code, arrival_airport_code, departure_time);

--
-- Name: idx_fraud_entity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fraud_entity ON public.fraud_scores USING btree (entity_type, entity_id);

--
-- Name: idx_hb_dates; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_hb_dates ON public.hotel_bookings USING btree (check_in_date, check_out_date);

--
-- Name: idx_hb_hotel; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_hb_hotel ON public.hotel_bookings USING btree (hotel_id);

--
-- Name: idx_hb_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_hb_order ON public.hotel_bookings USING btree (order_id);

--
-- Name: idx_hb_room; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_hb_room ON public.hotel_bookings USING btree (room_id);

--
-- Name: idx_hb_room_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_hb_room_type ON public.hotel_bookings USING btree (room_type_id);

--
-- Name: idx_hb_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_hb_status ON public.hotel_bookings USING btree (status);

--
-- Name: idx_hb_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_hb_user ON public.hotel_bookings USING btree (user_id);

--
-- Name: idx_hotel_amenities_amenity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_hotel_amenities_amenity ON public.hotel_amenities USING btree (amenity_id);

--
-- Name: idx_hotel_images_hotel; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_hotel_images_hotel ON public.hotel_images USING btree (hotel_id, sort_order);

--
-- Name: idx_hotels_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_hotels_active ON public.hotels USING btree (is_active) WHERE (is_active = true);

--
-- Name: idx_hotels_destination; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_hotels_destination ON public.hotels USING btree (destination_id);

--
-- Name: idx_hotels_managed_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_hotels_managed_by ON public.hotels USING btree (managed_by);

--
-- Name: idx_hotels_name_trgm; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_hotels_name_trgm ON public.hotels USING gin (name public.gin_trgm_ops);

--
-- Name: idx_hotels_star; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_hotels_star ON public.hotels USING btree (star_rating);

--
-- Name: idx_ii_itinerary; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ii_itinerary ON public.itinerary_items USING btree (itinerary_id, day_number, sort_order);

--
-- Name: idx_img_entity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_img_entity ON public.image_ai_metadata USING btree (entity_type, entity_id);

--
-- Name: idx_invoices_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_invoices_user ON public.invoices USING btree (user_id, issued_at DESC);

--
-- Name: idx_kd_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_kd_active ON public.knowledge_documents USING btree (is_active) WHERE is_active;

--
-- Name: idx_kd_active_language; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_kd_active_language ON public.knowledge_documents USING btree (is_active, language);

--
-- Name: idx_kd_sha; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_kd_sha ON public.knowledge_documents USING btree (content_sha256);

--
-- Name: idx_kd_source; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_kd_source ON public.knowledge_documents USING btree (source_type, source_id);

--
-- Name: idx_landmarks_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_landmarks_category ON public.landmarks USING btree (category);

--
-- Name: idx_landmarks_destination; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_landmarks_destination ON public.landmarks USING btree (destination_id);

--
-- Name: idx_landmarks_name_trgm; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_landmarks_name_trgm ON public.landmarks USING gin (name public.gin_trgm_ops);

--
-- Name: idx_notifications_user_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_user_time ON public.notifications USING btree (user_id, created_at DESC);

--
-- Name: idx_notifications_user_unread; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_user_unread ON public.notifications USING btree (user_id) WHERE (is_read = false);

--
-- Name: idx_oauth_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_oauth_user ON public.user_oauth_accounts USING btree (user_id);

--
-- Name: idx_orders_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orders_code ON public.orders USING btree (order_code);

--
-- Name: idx_orders_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orders_created ON public.orders USING btree (created_at DESC);

--
-- Name: idx_orders_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orders_status ON public.orders USING btree (status);

--
-- Name: idx_orders_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orders_user ON public.orders USING btree (user_id);

--
-- Name: idx_payments_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payments_order ON public.payments USING btree (order_id);

--
-- Name: idx_payments_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payments_status ON public.payments USING btree (status);

--
-- Name: idx_payments_txn; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payments_txn ON public.payments USING btree (transaction_id) WHERE (transaction_id IS NOT NULL);

--
-- Name: idx_ph_entity_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ph_entity_date ON public.price_history USING btree (entity_type, entity_id, check_date);

--
-- Name: idx_post_reactions_post; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_post_reactions_post ON public.post_reactions USING btree (post_id);

--
-- Name: idx_post_reactions_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_post_reactions_user ON public.post_reactions USING btree (user_id);

--
-- Name: idx_posts_entity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_posts_entity ON public.posts USING btree (entity_type, entity_id);

--
-- Name: idx_posts_rating; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_posts_rating ON public.posts USING btree (entity_type, entity_id, rating);

--
-- Name: idx_posts_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_posts_status ON public.posts USING btree (status);

--
-- Name: idx_posts_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_posts_user ON public.posts USING btree (user_id);

--
-- Name: idx_pw_reset_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pw_reset_user ON public.password_reset_tokens USING btree (user_id);

--
-- Name: idx_rec_user_score; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_rec_user_score ON public.recommendations USING btree (user_id, score DESC);

--
-- Name: idx_refresh_tokens_expires; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_refresh_tokens_expires ON public.refresh_tokens USING btree (expires_at) WHERE (revoked_at IS NULL);

--
-- Name: idx_refresh_tokens_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_refresh_tokens_user ON public.refresh_tokens USING btree (user_id);

--
-- Name: idx_role_perm_permission; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_role_perm_permission ON public.role_permissions USING btree (permission_id);

--
-- Name: idx_room_type_amenities_amenity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_room_type_amenities_amenity ON public.room_type_amenities USING btree (amenity_id);

--
-- Name: idx_room_types_hotel; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_room_types_hotel ON public.room_types USING btree (hotel_id);

--
-- Name: idx_room_types_name_trgm; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_room_types_name_trgm ON public.room_types USING gin (name public.gin_trgm_ops);

--
-- Name: idx_rooms_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_rooms_status ON public.rooms USING btree (status);

--
-- Name: idx_rooms_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_rooms_type ON public.rooms USING btree (room_type_id);

--
-- Name: idx_sq_text_trgm; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sq_text_trgm ON public.search_queries USING gin (query_text public.gin_trgm_ops);

--
-- Name: idx_sq_user_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sq_user_time ON public.search_queries USING btree (user_id, created_at DESC);

--
-- Name: idx_tour_bookings_tour_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tour_bookings_tour_date ON public.tour_bookings USING btree (tour_id, departure_date);

--
-- Name: idx_tour_bookings_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tour_bookings_user ON public.tour_bookings USING btree (user_id);

--
-- Name: idx_tours_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tours_active ON public.tours USING btree (is_active) WHERE is_active;

--
-- Name: idx_tours_destination; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tours_destination ON public.tours USING btree (destination_id);

--
-- Name: idx_ui_entity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ui_entity ON public.user_interactions USING btree (entity_type, entity_id);

--
-- Name: idx_ui_user_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ui_user_time ON public.user_interactions USING btree (user_id, created_at DESC);

--
-- Name: idx_user_roles_assigned_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_roles_assigned_by ON public.user_roles USING btree (assigned_by);

--
-- Name: idx_user_roles_role; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_roles_role ON public.user_roles USING btree (role_id);

--
-- Name: idx_users_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_active ON public.users USING btree (is_active) WHERE (deleted_at IS NULL);

--
-- Name: idx_users_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_email ON public.users USING btree (email);

--
-- Name: uq_chat_feedback_message_user; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_chat_feedback_message_user ON public.chat_feedback USING btree (message_id, user_id);

--
-- Name: uq_flight_bookings_seat_active; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_flight_bookings_seat_active ON public.flight_bookings USING btree (seat_id) WHERE ((seat_id IS NOT NULL) AND (status = ANY (ARRAY['pending'::public.booking_status, 'confirmed'::public.booking_status, 'completed'::public.booking_status])));

--
-- Name: comments trg_comments_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_comments_updated_at BEFORE UPDATE ON public.comments FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: continents trg_continents_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_continents_updated_at BEFORE UPDATE ON public.continents FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: countries trg_countries_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_countries_updated_at BEFORE UPDATE ON public.countries FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: hotel_bookings trg_hotel_bookings_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_hotel_bookings_updated_at BEFORE UPDATE ON public.hotel_bookings FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: hotels trg_hotels_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_hotels_updated_at BEFORE UPDATE ON public.hotels FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: orders trg_orders_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_orders_updated_at BEFORE UPDATE ON public.orders FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: posts trg_posts_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_posts_updated_at BEFORE UPDATE ON public.posts FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: posts trg_posts_validate_entity; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_posts_validate_entity BEFORE INSERT OR UPDATE OF entity_type, entity_id ON public.posts FOR EACH ROW EXECUTE FUNCTION public.validate_post_entity();

--
-- Name: rooms trg_rooms_sync_total; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_rooms_sync_total AFTER INSERT OR DELETE OR UPDATE OF room_type_id ON public.rooms FOR EACH ROW EXECUTE FUNCTION public.sync_hotel_total_rooms();

--
-- Name: users trg_users_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON public.users FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: audit_logs audit_logs_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE SET NULL;

--
-- Name: car_bookings car_bookings_car_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.car_bookings
    ADD CONSTRAINT car_bookings_car_id_fkey FOREIGN KEY (car_id) REFERENCES public.cars(id) ON DELETE RESTRICT;

--
-- Name: car_bookings car_bookings_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.car_bookings
    ADD CONSTRAINT car_bookings_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id) ON DELETE SET NULL;

--
-- Name: car_bookings car_bookings_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.car_bookings
    ADD CONSTRAINT car_bookings_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

--
-- Name: cars cars_destination_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cars
    ADD CONSTRAINT cars_destination_id_fkey FOREIGN KEY (destination_id) REFERENCES public.destinations(id) ON DELETE SET NULL;

--
-- Name: cars cars_managed_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cars
    ADD CONSTRAINT cars_managed_by_fkey FOREIGN KEY (managed_by) REFERENCES public.users(id) ON DELETE SET NULL;

--
-- Name: cart_items cart_items_cart_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT cart_items_cart_id_fkey FOREIGN KEY (cart_id) REFERENCES public.carts(id) ON DELETE CASCADE;

--
-- Name: cart_items cart_items_room_type_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT cart_items_room_type_id_fkey FOREIGN KEY (room_type_id) REFERENCES public.room_types(id) ON DELETE SET NULL;

--
-- Name: carts carts_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.carts
    ADD CONSTRAINT carts_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

--
-- Name: chat_feedback chat_feedback_message_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.chat_feedback
    ADD CONSTRAINT chat_feedback_message_id_fkey FOREIGN KEY (message_id) REFERENCES public.chat_messages(id) ON DELETE CASCADE;

--
-- Name: chat_feedback chat_feedback_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.chat_feedback
    ADD CONSTRAINT chat_feedback_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE SET NULL;

--
-- Name: chat_messages chat_messages_session_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.chat_messages
    ADD CONSTRAINT chat_messages_session_id_fkey FOREIGN KEY (session_id) REFERENCES public.chat_sessions(id) ON DELETE CASCADE;

--
-- Name: chat_sessions chat_sessions_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.chat_sessions
    ADD CONSTRAINT chat_sessions_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE SET NULL;

--
-- Name: comments comments_parent_comment_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT comments_parent_comment_id_fkey FOREIGN KEY (parent_comment_id) REFERENCES public.comments(id) ON DELETE CASCADE;

--
-- Name: comments comments_post_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT comments_post_id_fkey FOREIGN KEY (post_id) REFERENCES public.posts(id) ON DELETE CASCADE;

--
-- Name: comments comments_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT comments_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

--
-- Name: contact_messages contact_messages_replied_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contact_messages
    ADD CONSTRAINT contact_messages_replied_by_fkey FOREIGN KEY (replied_by) REFERENCES public.users(id) ON DELETE SET NULL;

--
-- Name: contact_messages contact_messages_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contact_messages
    ADD CONSTRAINT contact_messages_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE SET NULL;

--
-- Name: destinations destinations_country_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.destinations
    ADD CONSTRAINT destinations_country_id_fkey FOREIGN KEY (country_id) REFERENCES public.countries(id) ON DELETE RESTRICT;

--
-- Name: favorites favorites_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.favorites
    ADD CONSTRAINT favorites_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

--
-- Name: hotel_amenities fk_amenity; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hotel_amenities
    ADD CONSTRAINT fk_amenity FOREIGN KEY (amenity_id) REFERENCES public.amenities(id) ON DELETE CASCADE;

--
-- Name: room_type_amenities fk_amenity_room; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.room_type_amenities
    ADD CONSTRAINT fk_amenity_room FOREIGN KEY (amenity_id) REFERENCES public.amenities(id) ON DELETE CASCADE;

--
-- Name: countries fk_country_continent; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.countries
    ADD CONSTRAINT fk_country_continent FOREIGN KEY (continent_id) REFERENCES public.continents(id) ON DELETE RESTRICT;

--
-- Name: hotel_amenities fk_hotel; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hotel_amenities
    ADD CONSTRAINT fk_hotel FOREIGN KEY (hotel_id) REFERENCES public.hotels(id) ON DELETE CASCADE;

--
-- Name: room_type_amenities fk_room_type; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.room_type_amenities
    ADD CONSTRAINT fk_room_type FOREIGN KEY (room_type_id) REFERENCES public.room_types(id) ON DELETE CASCADE;

--
-- Name: flight_bookings flight_bookings_flight_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flight_bookings
    ADD CONSTRAINT flight_bookings_flight_id_fkey FOREIGN KEY (flight_id) REFERENCES public.flights(id) ON DELETE RESTRICT;

--
-- Name: flight_bookings flight_bookings_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flight_bookings
    ADD CONSTRAINT flight_bookings_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id) ON DELETE SET NULL;

--
-- Name: flight_bookings flight_bookings_seat_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flight_bookings
    ADD CONSTRAINT flight_bookings_seat_id_fkey FOREIGN KEY (seat_id) REFERENCES public.flight_seats(id) ON DELETE SET NULL;

--
-- Name: flight_bookings flight_bookings_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flight_bookings
    ADD CONSTRAINT flight_bookings_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

--
-- Name: flight_seats flight_seats_flight_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flight_seats
    ADD CONSTRAINT flight_seats_flight_id_fkey FOREIGN KEY (flight_id) REFERENCES public.flights(id) ON DELETE CASCADE;

--
-- Name: hotel_bookings hotel_bookings_hotel_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hotel_bookings
    ADD CONSTRAINT hotel_bookings_hotel_id_fkey FOREIGN KEY (hotel_id) REFERENCES public.hotels(id) ON DELETE RESTRICT;

--
-- Name: hotel_bookings hotel_bookings_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hotel_bookings
    ADD CONSTRAINT hotel_bookings_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id) ON DELETE RESTRICT;

--
-- Name: hotel_bookings hotel_bookings_room_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hotel_bookings
    ADD CONSTRAINT hotel_bookings_room_id_fkey FOREIGN KEY (room_id) REFERENCES public.rooms(id) ON DELETE SET NULL;

--
-- Name: hotel_bookings hotel_bookings_room_type_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hotel_bookings
    ADD CONSTRAINT hotel_bookings_room_type_id_fkey FOREIGN KEY (room_type_id) REFERENCES public.room_types(id) ON DELETE RESTRICT;

--
-- Name: hotel_bookings hotel_bookings_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hotel_bookings
    ADD CONSTRAINT hotel_bookings_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE RESTRICT;

--
-- Name: hotel_images hotel_images_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hotel_images
    ADD CONSTRAINT hotel_images_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id) ON DELETE SET NULL;

--
-- Name: hotel_images hotel_images_hotel_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hotel_images
    ADD CONSTRAINT hotel_images_hotel_id_fkey FOREIGN KEY (hotel_id) REFERENCES public.hotels(id) ON DELETE CASCADE;

--
-- Name: hotels hotels_destination_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hotels
    ADD CONSTRAINT hotels_destination_id_fkey FOREIGN KEY (destination_id) REFERENCES public.destinations(id) ON DELETE RESTRICT;

--
-- Name: hotels hotels_managed_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hotels
    ADD CONSTRAINT hotels_managed_by_fkey FOREIGN KEY (managed_by) REFERENCES public.users(id) ON DELETE SET NULL;

--
-- Name: invoices invoices_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT invoices_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id) ON DELETE RESTRICT;

--
-- Name: invoices invoices_payment_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT invoices_payment_id_fkey FOREIGN KEY (payment_id) REFERENCES public.payments(id) ON DELETE SET NULL;

--
-- Name: invoices invoices_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT invoices_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE RESTRICT;

--
-- Name: itineraries itineraries_destination_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.itineraries
    ADD CONSTRAINT itineraries_destination_id_fkey FOREIGN KEY (destination_id) REFERENCES public.destinations(id) ON DELETE SET NULL;

--
-- Name: itineraries itineraries_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.itineraries
    ADD CONSTRAINT itineraries_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE SET NULL;

--
-- Name: itinerary_items itinerary_items_itinerary_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.itinerary_items
    ADD CONSTRAINT itinerary_items_itinerary_id_fkey FOREIGN KEY (itinerary_id) REFERENCES public.itineraries(id) ON DELETE CASCADE;

--
-- Name: knowledge_chunks knowledge_chunks_document_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.knowledge_chunks
    ADD CONSTRAINT knowledge_chunks_document_id_fkey FOREIGN KEY (document_id) REFERENCES public.knowledge_documents(id) ON DELETE CASCADE;

--
-- Name: landmarks landmarks_destination_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.landmarks
    ADD CONSTRAINT landmarks_destination_id_fkey FOREIGN KEY (destination_id) REFERENCES public.destinations(id) ON DELETE CASCADE;

--
-- Name: notifications notifications_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

--
-- Name: orders orders_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE RESTRICT;

--
-- Name: password_reset_tokens password_reset_tokens_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

--
-- Name: payments payments_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id) ON DELETE RESTRICT;

--
-- Name: post_reactions post_reactions_post_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.post_reactions
    ADD CONSTRAINT post_reactions_post_id_fkey FOREIGN KEY (post_id) REFERENCES public.posts(id) ON DELETE CASCADE;

--
-- Name: post_reactions post_reactions_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.post_reactions
    ADD CONSTRAINT post_reactions_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

--
-- Name: posts posts_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.posts
    ADD CONSTRAINT posts_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

--
-- Name: recommendations recommendations_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recommendations
    ADD CONSTRAINT recommendations_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

--
-- Name: refresh_tokens refresh_tokens_replaced_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_replaced_by_fkey FOREIGN KEY (replaced_by) REFERENCES public.refresh_tokens(id) ON DELETE SET NULL;

--
-- Name: refresh_tokens refresh_tokens_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

--
-- Name: review_ai_analysis review_ai_analysis_post_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.review_ai_analysis
    ADD CONSTRAINT review_ai_analysis_post_id_fkey FOREIGN KEY (post_id) REFERENCES public.posts(id) ON DELETE CASCADE;

--
-- Name: role_permissions role_permissions_permission_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT role_permissions_permission_id_fkey FOREIGN KEY (permission_id) REFERENCES public.permissions(id) ON DELETE CASCADE;

--
-- Name: role_permissions role_permissions_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT role_permissions_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.roles(id) ON DELETE CASCADE;

--
-- Name: room_availability room_availability_room_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.room_availability
    ADD CONSTRAINT room_availability_room_id_fkey FOREIGN KEY (room_id) REFERENCES public.rooms(id) ON DELETE CASCADE;

--
-- Name: room_types room_types_hotel_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.room_types
    ADD CONSTRAINT room_types_hotel_id_fkey FOREIGN KEY (hotel_id) REFERENCES public.hotels(id) ON DELETE CASCADE;

--
-- Name: rooms rooms_room_type_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rooms
    ADD CONSTRAINT rooms_room_type_id_fkey FOREIGN KEY (room_type_id) REFERENCES public.room_types(id) ON DELETE CASCADE;

--
-- Name: search_queries search_queries_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.search_queries
    ADD CONSTRAINT search_queries_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE SET NULL;

--
-- Name: tour_bookings tour_bookings_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tour_bookings
    ADD CONSTRAINT tour_bookings_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id) ON DELETE SET NULL;

--
-- Name: tour_bookings tour_bookings_tour_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tour_bookings
    ADD CONSTRAINT tour_bookings_tour_id_fkey FOREIGN KEY (tour_id) REFERENCES public.tours(id) ON DELETE RESTRICT;

--
-- Name: tour_bookings tour_bookings_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tour_bookings
    ADD CONSTRAINT tour_bookings_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

--
-- Name: tour_cars tour_cars_car_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tour_cars
    ADD CONSTRAINT tour_cars_car_id_fkey FOREIGN KEY (car_id) REFERENCES public.cars(id) ON DELETE CASCADE;

--
-- Name: tour_cars tour_cars_tour_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tour_cars
    ADD CONSTRAINT tour_cars_tour_id_fkey FOREIGN KEY (tour_id) REFERENCES public.tours(id) ON DELETE CASCADE;

--
-- Name: tour_flights tour_flights_flight_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tour_flights
    ADD CONSTRAINT tour_flights_flight_id_fkey FOREIGN KEY (flight_id) REFERENCES public.flights(id) ON DELETE CASCADE;

--
-- Name: tour_flights tour_flights_tour_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tour_flights
    ADD CONSTRAINT tour_flights_tour_id_fkey FOREIGN KEY (tour_id) REFERENCES public.tours(id) ON DELETE CASCADE;

--
-- Name: tour_hotels tour_hotels_hotel_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tour_hotels
    ADD CONSTRAINT tour_hotels_hotel_id_fkey FOREIGN KEY (hotel_id) REFERENCES public.hotels(id) ON DELETE CASCADE;

--
-- Name: tour_hotels tour_hotels_tour_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tour_hotels
    ADD CONSTRAINT tour_hotels_tour_id_fkey FOREIGN KEY (tour_id) REFERENCES public.tours(id) ON DELETE CASCADE;

--
-- Name: tours tours_destination_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tours
    ADD CONSTRAINT tours_destination_id_fkey FOREIGN KEY (destination_id) REFERENCES public.destinations(id) ON DELETE SET NULL;

--
-- Name: user_interactions user_interactions_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_interactions
    ADD CONSTRAINT user_interactions_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE SET NULL;

--
-- Name: user_oauth_accounts user_oauth_accounts_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_oauth_accounts
    ADD CONSTRAINT user_oauth_accounts_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

--
-- Name: user_preferences user_preferences_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_preferences
    ADD CONSTRAINT user_preferences_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

--
-- Name: user_roles user_roles_assigned_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_assigned_by_fkey FOREIGN KEY (assigned_by) REFERENCES public.users(id) ON DELETE SET NULL;

--
-- Name: user_roles user_roles_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.roles(id) ON DELETE CASCADE;

--
-- Name: user_roles user_roles_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

--
-- Name: SCHEMA chatbot; Type: ACL; Schema: -; Owner: -
--

GRANT USAGE ON SCHEMA chatbot TO chatbot_ro;

--
-- Name: FUNCTION norm(t text); Type: ACL; Schema: chatbot; Owner: -
--

GRANT ALL ON FUNCTION chatbot.norm(t text) TO chatbot_ro;

--
-- Name: TABLE cars; Type: ACL; Schema: chatbot; Owner: -
--

GRANT SELECT ON TABLE chatbot.cars TO chatbot_ro;

--
-- Name: TABLE destinations; Type: ACL; Schema: chatbot; Owner: -
--

GRANT SELECT ON TABLE chatbot.destinations TO chatbot_ro;

--
-- Name: TABLE flights; Type: ACL; Schema: chatbot; Owner: -
--

GRANT SELECT ON TABLE chatbot.flights TO chatbot_ro;

--
-- Name: TABLE hotel_amenities; Type: ACL; Schema: chatbot; Owner: -
--

GRANT SELECT ON TABLE chatbot.hotel_amenities TO chatbot_ro;

--
-- Name: TABLE hotels; Type: ACL; Schema: chatbot; Owner: -
--

GRANT SELECT ON TABLE chatbot.hotels TO chatbot_ro;

--
-- Name: TABLE landmarks; Type: ACL; Schema: chatbot; Owner: -
--

GRANT SELECT ON TABLE chatbot.landmarks TO chatbot_ro;

--
-- Name: TABLE room_types; Type: ACL; Schema: chatbot; Owner: -
--

GRANT SELECT ON TABLE chatbot.room_types TO chatbot_ro;

--
-- Name: TABLE tours; Type: ACL; Schema: chatbot; Owner: -
--

GRANT SELECT ON TABLE chatbot.tours TO chatbot_ro;

--
-- Name: TABLE knowledge_chunks; Type: ACL; Schema: public; Owner: -
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.knowledge_chunks TO chatbot_rag;

--
-- Name: TABLE knowledge_documents; Type: ACL; Schema: public; Owner: -
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.knowledge_documents TO chatbot_rag;

--
-- PostgreSQL database dump complete
--
