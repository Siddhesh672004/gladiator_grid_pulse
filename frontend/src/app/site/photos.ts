// free pictures from Wikimedia Commons, loaded by link (nothing is stored in the project)
// their licences ask for the author's name: PHOTO_CREDITS is shown in the page footer
const COMMONS = 'https://thumb.wikimedia.org/wikipedia/commons/thumb/';

export const PHOTOS: { [name: string]: string } = {
  race: COMMONS + '6/6e/GT_Race_2_-_British_GT_Championship_-_Lamborghini_%2851372322792%29.jpg/1280px-GT_Race_2_-_British_GT_Championship_-_Lamborghini_%2851372322792%29.jpg',
  paddock: COMMONS + 'f/fd/American_Le_Mans_Series_Oak_Tree_Grand_Prix_at_VIR_October_2013_%2810295514404%29.jpg/1280px-American_Le_Mans_Series_Oak_Tree_Grand_Prix_at_VIR_October_2013_%2810295514404%29.jpg',
  karting: COMMONS + '8/88/Mika_Salo_Circuit%2C_go-karts.jpg/1280px-Mika_Salo_Circuit%2C_go-karts.jpg',
  cup: COMMONS + '5/53/Carl_Cavers_Porsche_Carrera_Cup_GB_Race_2_Thruxton_2026.jpg/1280px-Carl_Cavers_Porsche_Carrera_Cup_GB_Race_2_Thruxton_2026.jpg'
};

// their real height at the 1280px width above, for <img width height> (the browser keeps their room before they arrive)
export const PHOTO_WIDTH = 1280;
export const PHOTO_HEIGHTS: { [name: string]: number } = { race: 720, paddock: 853, karting: 853, cup: 720 };

export const PHOTO_CREDITS = [
  { photo: 'GT race', author: '35mmMan', licence: 'CC BY 2.0', link: 'https://commons.wikimedia.org/wiki/File:GT_Race_2_-_British_GT_Championship_-_Lamborghini_(51372322792).jpg' },
  { photo: 'Le Mans Series car', author: 'Trailers of the East Coast', licence: 'CC BY 2.0', link: 'https://commons.wikimedia.org/wiki/File:American_Le_Mans_Series_Oak_Tree_Grand_Prix_at_VIR_October_2013_(10295514404).jpg' },
  { photo: 'Karting circuit', author: 'Manster323', licence: 'CC0', link: 'https://commons.wikimedia.org/wiki/File:Mika_Salo_Circuit,_go-karts.jpg' },
  { photo: 'Carrera Cup car', author: 'BWard 1997', licence: 'CC BY 4.0', link: 'https://commons.wikimedia.org/wiki/File:Carl_Cavers_Porsche_Carrera_Cup_GB_Race_2_Thruxton_2026.jpg' }
];
