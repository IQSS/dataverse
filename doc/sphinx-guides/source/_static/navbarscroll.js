/*  
    Use to fix hidden section headers behind the navbar when using links with targets
    See: https://stackoverflow.com/questions/10732690/offsetting-an-html-anchor-to-adjust-for-fixed-header    
*/
$jqTheme(document).ready(function() {
  $jqTheme('a[href*="#"]:not([href="#"])').on('click', function() {
    if (location.pathname.replace(/^\//,'') == this.pathname.replace(/^\//,'') 
&& location.hostname == this.hostname) {
      var target = $jqTheme(this.hash);
      target = target.length ? target : $jqTheme('a.headerlink[href="#' + this.hash.slice(1) +'""]');
      if (target.length) {
        $jqTheme('html,body').animate({
          scrollTop: target.offset().top - 60 //offsets for fixed header
        }, 1000);
        return false;
      }
    }
  });
  //Executed on page load with URL containing an anchor tag.
  if($jqTheme(location.href.split("#")[1])) {
      var target = $jqTheme('#'+location.href.split("#")[1]);
      if (target.length) {
        $jqTheme('html,body').animate({
          scrollTop: target.offset().top - 60 //offset height of header here too.
        }, 1000);
        return false;
      }
    }
});