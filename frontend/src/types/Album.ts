export interface Album {
    id: string;
    name: string;
    artists: string[];
    totalTracks: number;
    imageUrl?: string;
    releaseDate?: string;
}
