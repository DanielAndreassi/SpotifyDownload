export interface Track {
    id: string;
    name: string;
    artists: { name: string }[];
    album: { name: string, images: { url: string }[] };
    duration_ms: number;
}
