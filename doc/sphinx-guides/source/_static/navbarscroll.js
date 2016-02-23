/*  
    Use to fix hidden section headers behind the navbar when using links with targets
    See: http://stackoverflow.com/questions/10732690/offsetting-an-html-anchor-to-adjust-for-fixed-header    
*/
(function($) {
$(function() {
  $('a[href*=#]:not([href=#])').on('click', function() {
    if (location.pathname.replace(/^\//,'') == this.pathname.replace(/^\//,'') 
&& location.hostname == this.hostname) {
      var target = $(this.hash);
      target = target.length ? target : $('a.headerlink[href="#' + this.hash.slice(1) +'""]');
      if (target.length) {
        $('html,body').animate({
          scrollTop: target.offset().top - 60 //offsets for fixed header
        }, 1000);
        return false;
      }
    }
  });
  //Executed on page load with URL containing an anchor tag.
  if($(location.href.split("#")[1])) {
      var target = $('#'+location.href.split("#")[1]);
      if (target.length) {
        $('html,body').animate({
          scrollTop: target.offset().top - 60 //offset height of header here too.
        }, 1000);
        return false;
      }
    }
});
})(jQuery);