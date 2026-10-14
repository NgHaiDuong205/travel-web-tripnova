-- Thư viện ảnh theo hạng phòng (các phòng cùng hạng dùng chung ảnh), tương tự hotel_images.
-- room_types.cover_image_url vẫn là ảnh đại diện (đặt từ thư viện qua API .../images/{id}/cover/).
CREATE TABLE public.room_type_images (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    room_type_id uuid NOT NULL,
    url text NOT NULL,
    caption character varying(255),
    sort_order integer DEFAULT 0 NOT NULL,
    created_by uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT room_type_images_pkey PRIMARY KEY (id),
    CONSTRAINT room_type_images_room_type_id_fkey FOREIGN KEY (room_type_id)
        REFERENCES public.room_types(id) ON DELETE CASCADE,
    CONSTRAINT room_type_images_created_by_fkey FOREIGN KEY (created_by)
        REFERENCES public.users(id) ON DELETE SET NULL
);

CREATE INDEX idx_room_type_images_room_type ON public.room_type_images USING btree (room_type_id, sort_order);
